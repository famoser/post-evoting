/* Copyright  (c) 2002 Graz University of Technology. All rights reserved.
 *
 * Redistribution and use in  source and binary forms, with or without
 * modification, are permitted  provided that the following conditions are met:
 *
 * 1. Redistributions of  source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in  binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment:
 *
 *    "This product includes software developed by IAIK of Graz University of
 *     Technology."
 *
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Graz University of Technology" and "IAIK of Graz University of
 *    Technology" must not be used to endorse or promote products derived from
 *    this software without prior written permission.
 *
 * 5. Products derived from this software may not be called
 *    "IAIK PKCS Wrapper", nor may "IAIK" appear in their name, without prior
 *    written permission of Graz University of Technology.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE LICENSOR BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY  OF SUCH DAMAGE.
 */

#include "pkcs11wrapper.h"
#include "getattributevalue.h"
#include "../include/pkcs11t.h"

/* ************************************************************************** */
/* The native implementation of the method GetAttributeValue                  */
/* and used helper functions                                                  */
/* ************************************************************************** */

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetAttributeValue
 * Signature: (JJ[Liaik/pkcs/pkcs11/wrapper/CK_ATTRIBUTE;Z)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jlong jObjectHandle         CK_OBJECT_HANDLE hObject
 * @param   jobjectArray jTemplate      CK_ATTRIBUTE_PTR pTemplate
 *                                      CK_ULONG ulCount
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetAttributeValue
(JNIEnv *env, jobject obj, jlong jSessionHandle, jlong jObjectHandle, jobjectArray jTemplate, jboolean jUseUtf8)
{
	CK_SESSION_HANDLE ckSessionHandle;
	CK_OBJECT_HANDLE ckObjectHandle;
	CK_ATTRIBUTE_PTR ckpAttributes, arrayAttributes  = NULL_PTR;
	CK_ULONG ckAttributesLength;
	CK_ULONG i, j;
	jobject jAttribute;
	CK_RV rv;
	int rv2;
	CK_ULONG arrayAttributesLength;
	ModuleData *moduleData;
	CK_FUNCTION_LIST_PTR ckpFunctions;
	signed long signedLength;

	TRACE0(tag_call, __FUNCTION__, "entering");

	moduleData = getModuleEntry(env, obj);
	if (moduleData == NULL_PTR) { throwDisconnectedRuntimeException(env); return; }
	ckpFunctions = getFunctionList(env, moduleData);
	if (ckpFunctions == NULL_PTR) { return; }

	TRACE2(tag_debug, __FUNCTION__, "hSession=%d, hObject=%u", (int)jSessionHandle, (unsigned int)jObjectHandle);

	ckSessionHandle = jLongToCKULong(jSessionHandle);
	ckObjectHandle = jLongToCKULong(jObjectHandle);
	TRACE1(tag_debug, __FUNCTION__,"jAttributeArrayToCKAttributeArray now with jTemplate = %p", jTemplate);
	if (jAttributeArrayToCKAttributeArray(env, jTemplate, &ckpAttributes, &ckAttributesLength, jUseUtf8)) { return; }
	TRACE2(tag_debug, __FUNCTION__,"jAttributeArrayToCKAttributeArray finished with ckpAttribute = %p, Length = %d\n", ckpAttributes, (unsigned int)ckAttributesLength);

	/* first set all pValue to NULL_PTR, to get the needed buffer length */
	for (i = 0; i < ckAttributesLength; i++) {
		freeAttributeValue(ckpAttributes, i, CK_TRUE);
	}

	/* get a copy of array attributes - as they need to be handled specially */
	arrayAttributesLength = 0;
	TRACE0(tag_call, __FUNCTION__, "find number of array attributes");
	for (i = 0; i < ckAttributesLength; i++) {
		// cka_allowed_mechanisms is not an attribute array but a mechanism array
		// if ((ckpAttributes[i].type & CKF_ARRAY_ATTRIBUTE) == CKF_ARRAY_ATTRIBUTE){
		if ((ckpAttributes[i].type == CKA_WRAP_TEMPLATE) || (ckpAttributes[i].type == CKA_UNWRAP_TEMPLATE)){
			arrayAttributesLength++;
		}
	}

	if(arrayAttributesLength > 0){
		TRACE1(tag_call, __FUNCTION__, "allocate %d bytes for array attributes copy", (unsigned int) (arrayAttributesLength * sizeof(CK_ATTRIBUTE)));
		arrayAttributes = (CK_ATTRIBUTE_PTR) malloc(arrayAttributesLength * sizeof(CK_ATTRIBUTE));
		if (arrayAttributes == NULL_PTR && arrayAttributesLength!=0) {
			throwOutOfMemoryError(env);
			/* free previously allocated memory*/
			freeAttributeArray(&ckpAttributes, ckAttributesLength, CK_FALSE);
			TRACE0(tag_call, __FUNCTION__, "exiting ");
			return;
		}
		// set to null_ptr to be able to correctly call freeAttributeValue later
		for (i = 0; i < arrayAttributesLength; i++) {
			arrayAttributes[i].pValue = NULL_PTR;
		}
	}

	TRACE0(tag_call, __FUNCTION__, "allocating memory of estimated size");
	rv = preAllocateAttributeArrayValues(env, __FUNCTION__, ckpAttributes, ckAttributesLength, arrayAttributes, arrayAttributesLength);
	if(rv == EXIT_MEM_FAILURE){
		TRACE0(tag_call, __FUNCTION__, "out of memory error - try standard method");
		/* free inner attribute array pointers only once, only in copy */
		freeAttributeArray(&arrayAttributes, arrayAttributesLength, CK_TRUE);
		arrayAttributesLength = 0;
		/* reset attributes to null_ptr */
		for (i = 0; i < ckAttributesLength; i++) {
			freeAttributeValue(ckpAttributes, i, CK_FALSE);
		}
		rv2 = getAttributeValuesStd(env, obj, &rv, ckSessionHandle, ckObjectHandle, ckpAttributes, ckAttributesLength);
		if(rv2 == EXIT_FAILURE){
			freeAttributeArray(&ckpAttributes, ckAttributesLength, CK_TRUE);
			TRACE0(tag_call, __FUNCTION__, "exiting ");
			return ;
		}
	}else{
		/* now get the attributes with all values */
		TRACE0(tag_debug, __FUNCTION__, "- going to get all values");
		rv = (*ckpFunctions->C_GetAttributeValue)(ckSessionHandle, ckObjectHandle, ckpAttributes, ckAttributesLength);
		for (i = 0; i < ckAttributesLength; i++) {
            TRACE2(tag_debug, __FUNCTION__, "size after call for %ld: %ld", i, (ckpAttributes+i)->ulValueLen);
		}
        TRACE0(tag_debug, __FUNCTION__, "- going to get all values");

		if(rv != CKR_OK) {
			TRACE1(tag_debug, __FUNCTION__, "smartcard returned error: %ld - read all with getAttributeValueStd", rv);
			freeAttributeArray(&arrayAttributes, arrayAttributesLength, CK_TRUE);
			for (i = 0; i < ckAttributesLength; i++) {
				freeAttributeValue(ckpAttributes, i, CK_FALSE);
			}
			arrayAttributesLength = 0;
			rv2 = getAttributeValuesStd(env, obj, &rv, ckSessionHandle, ckObjectHandle, ckpAttributes, ckAttributesLength);
			if(rv2 == EXIT_FAILURE){
				freeAttributeArray(&ckpAttributes, ckAttributesLength, CK_TRUE);
				TRACE0(tag_call, __FUNCTION__, "exiting ");
				return ;
			}
		}

		/* check if module really supports array attributes */
		if(arrayAttributesLength > 0){
			TRACE0(tag_call, __FUNCTION__, "check array-attributes pointers");
			j = 0;
			for (i = 0; i < ckAttributesLength; i++) {
				// cka_allowed_mechanisms is not an attribute array but a mechanism array
				// if ((ckpAttributes[i].type & CKF_ARRAY_ATTRIBUTE) == CKF_ARRAY_ATTRIBUTE){
				if ((ckpAttributes[i].type == CKA_WRAP_TEMPLATE) || (ckpAttributes[i].type == CKA_UNWRAP_TEMPLATE)){
					signedLength = ckpAttributes[i].ulValueLen;
					if(signedLength > 0){
						// if module doesn't support ARRAY ATTRIBUTES, value set to NULL_PTR
						checkArrayAttributePointers(&ckpAttributes[i], &arrayAttributes[j]);
					}
					j++;
				}
			}
		}
	}

	/* copy back the values to the Java attributes */
	TRACE0(tag_call, __FUNCTION__, "convert attributes to java objects");
	for (i = 0; i < ckAttributesLength; i++) {
		signedLength = ckpAttributes[i].ulValueLen;
		if (signedLength == -1){ // mark that length was -1 that way
			jAttribute = NULL_PTR;
		}else{
			jAttribute = ckAttributePtrToJAttribute(env, &(ckpAttributes[i]), obj, jSessionHandle, jObjectHandle, jUseUtf8);
		}
		(*env)->SetObjectArrayElement(env, jTemplate, i, jAttribute);
	}

	if(ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
		TRACE0(tag_info, __FUNCTION__,"rv != OK\n");
	}

	if(arrayAttributes == NULL_PTR){
		freeAttributeArray(&ckpAttributes, ckAttributesLength, CK_TRUE);
	} else {
		/* free inner attribute array pointers only once, only in copy */
		freeAttributeArray(&ckpAttributes, ckAttributesLength, CK_FALSE);
		freeAttributeArray(&arrayAttributes, arrayAttributesLength, CK_TRUE);
	}
	TRACE0(tag_call, __FUNCTION__, "exiting ");
	return;
}

/*
 * note:
 * precondition: attributes must have correct format (pValue = NULL_PTR and ulValueLen=0)
 * Exception: (un)wrap template: may also have correctly allocated memory for given ulValueLen).
 * But inner attributes must have pValue = NULL_PTR and ulValueLen=0 in such a case.
 */
int getAttributeValuesStd(JNIEnv *env, jobject obj, CK_RV *rv, CK_SESSION_HANDLE ckSessionHandle, CK_OBJECT_HANDLE ckObjectHandle, CK_ATTRIBUTE_PTR ckpAttributes, CK_ULONG ckAttributesLength)
{
	CK_ATTRIBUTE_PTR ckAttributeArray = NULL_PTR;
	CK_ULONG ckBufferLength;
	CK_ULONG length;
	CK_ULONG i, j;
	ModuleData *moduleData;
	CK_FUNCTION_LIST_PTR ckpFunctions;
	signed long signedLength;
	CK_BBOOL arrayAttribute;

	TRACE0(tag_call, __FUNCTION__, "entering");

	moduleData = getModuleEntry(env, obj);
	if (moduleData == NULL_PTR) { throwDisconnectedRuntimeException(env); return EXIT_FAILURE; }
	ckpFunctions = getFunctionList(env, moduleData);
	if (ckpFunctions == NULL_PTR) { return EXIT_FAILURE; }

	TRACE2(tag_debug, __FUNCTION__, "hSession=%d, hObject=%u", (int)ckSessionHandle, (unsigned int)ckObjectHandle);

	TRACE0(tag_debug, __FUNCTION__, "- going to get buffer sizes");
	(*rv) = (*ckpFunctions->C_GetAttributeValue)(ckSessionHandle, ckObjectHandle, ckpAttributes, ckAttributesLength);
	if (ckAssertAttributeReturnValueOK(env, (*rv), __FUNCTION__, ckAttributesLength) != CK_ASSERT_OK) {
		TRACE0(tag_call, __FUNCTION__, "exiting ");
		return EXIT_FAILURE;
	}

	arrayAttribute = FALSE;
	for (i = 0; i < ckAttributesLength; i++) {
		signedLength = ckpAttributes[i].ulValueLen;
		if (signedLength != -1){
			// cka_allowed_mechanisms is not an attribute array but a mechanism array
			// if ((ckpAttributes[i].type & CKF_ARRAY_ATTRIBUTE) == CKF_ARRAY_ATTRIBUTE){
			if ((ckpAttributes[i].type == CKA_WRAP_TEMPLATE) || (ckpAttributes[i].type == CKA_UNWRAP_TEMPLATE)){
				// whole array attribute may has already been read, only allocate if null
				if(ckpAttributes[i].pValue == NULL_PTR){
					// allocate array
					arrayAttribute = TRUE;
					ckBufferLength = sizeof(CK_BYTE) * ckpAttributes[i].ulValueLen;
					ckpAttributes[i].pValue = (CK_ATTRIBUTE_PTR) malloc(ckBufferLength);
					ckpAttributes[i].ulValueLen = ckBufferLength;

					// clean up if array could not be allocated
					if (ckpAttributes[i].pValue == NULL_PTR && ckBufferLength!=0) {
						/* free previously allocated memory*/
						throwOutOfMemoryError(env);
						TRACE0(tag_call, __FUNCTION__, "exiting ");
						return EXIT_FAILURE;
					}

					// initialize array to hold NULL_PTRs
					ckAttributeArray = (CK_ATTRIBUTE_PTR)ckpAttributes[i].pValue;
					length = ckpAttributes[i].ulValueLen/sizeof(CK_ATTRIBUTE);
					for (j=0; j<length; j++){
						ckAttributeArray[j].pValue = NULL_PTR;
					}
				}
			}
		}
	}

	// get ulValueLen of the attributes of an array attribute if present
	if(arrayAttribute == TRUE){
		//reset length for next native call to avoid any errors, as length must always reflect allocated memory in pValue
		for (i = 0; i < ckAttributesLength; i++) {
			if (ckpAttributes[i].pValue == NULL_PTR){
				ckpAttributes[i].ulValueLen = 0;
			}
		}

		TRACE0(tag_debug, __FUNCTION__, "- going to get buffer sizes of nested CKF_ARRAY_ATTRIBUTE if present");
		(*rv) = (*ckpFunctions->C_GetAttributeValue)(ckSessionHandle, ckObjectHandle, ckpAttributes, ckAttributesLength);
		if(ckAssertAttributeReturnValueOK(env, (*rv), __FUNCTION__, ckAttributesLength) != CK_ASSERT_OK) {
			TRACE0(tag_call, __FUNCTION__, "exiting ");
			return EXIT_FAILURE;
		}
	}

	/* now, the ulValueLength field of each attribute should hold the exact buffer length needed
	 * to allocate the needed buffers accordingly
	 */
	for (i = 0; i < ckAttributesLength; i++) {
		signedLength = ckpAttributes[i].ulValueLen;
		if(signedLength != -1){
			// cka_allowed_mechanisms is not an attribute array but a mechanism array
			// if ((ckpAttributes[i].type & CKF_ARRAY_ATTRIBUTE) == CKF_ARRAY_ATTRIBUTE){
			if ((ckpAttributes[i].type == CKA_WRAP_TEMPLATE) || (ckpAttributes[i].type == CKA_UNWRAP_TEMPLATE)){
				TRACE0(tag_debug, __FUNCTION__, "- found attribute array. going to initialize the buffers of the array.");
				ckAttributeArray = (CK_ATTRIBUTE_PTR)ckpAttributes[i].pValue;
				length = ckpAttributes[i].ulValueLen/sizeof(CK_ATTRIBUTE);
				if(length > 0){
					// if module doesn't support ARRAY ATTRIBUTES, the pointer was saved as byte array
					// and is therefore no valid pointer to a value.
					// should already occur with first index
					// Attribute value is set to NULL in that case.
					if(ckAttributeArray[0].pValue != NULL_PTR){
						TRACE0(tag_debug, __FUNCTION__, "  Module does not support ARRAY_ATTRIBUTES. Thus, the attribute value is set to NULL.");
						freeAttributeValue(ckpAttributes, i, CK_FALSE);
						ckpAttributes[i].ulValueLen = 0;
						continue;
					}

					TRACE1(tag_debug, __FUNCTION__,"allocate mem for attributes in attribute array, length of attribute array = %u\n", (unsigned int)ckpAttributes[i].ulValueLen);
					for (j=0; j<length; j++){
						signedLength = ckAttributeArray[j].ulValueLen;
						if (signedLength != -1){
							ckBufferLength = sizeof(CK_BYTE) * ckAttributeArray[j].ulValueLen;
							ckAttributeArray[j].pValue = (void *) malloc(ckBufferLength);
							ckAttributeArray[j].ulValueLen = ckBufferLength;
							if ((ckAttributeArray[j].pValue == NULL_PTR && ckBufferLength!=0)) {
								throwOutOfMemoryError(env);
								TRACE0(tag_call, __FUNCTION__, "exiting ");
								return EXIT_FAILURE;
							}
						}
					}
				}
			} else{
				TRACE2(tag_debug, __FUNCTION__,"allocate mem for attribute type %u, length of attribute = %u\n", (unsigned int)ckpAttributes[i].type, (unsigned int)ckpAttributes[i].ulValueLen);
				ckBufferLength = sizeof(CK_BYTE) * ckpAttributes[i].ulValueLen;
				ckpAttributes[i].pValue = (void *) malloc(ckBufferLength);
				ckpAttributes[i].ulValueLen = ckBufferLength;
				if (ckpAttributes[i].pValue == NULL_PTR && ckBufferLength!=0){
					throwOutOfMemoryError(env);
					TRACE0(tag_call, __FUNCTION__, "exiting ");
					return EXIT_FAILURE;
				}
			}
		}else{
			TRACE1(tag_debug, __FUNCTION__,"do not allocate mem for attribute type %u, length is -1\n", (unsigned int)ckpAttributes[i].type);
			ckpAttributes[i].ulValueLen = 0;
		}
	}

	/* now get the attributes with all values */
	TRACE0(tag_debug, __FUNCTION__, "- going to get all values");
	(*rv) = (*ckpFunctions->C_GetAttributeValue)(ckSessionHandle, ckObjectHandle, ckpAttributes, ckAttributesLength);
	TRACE0(tag_info, __FUNCTION__,"done");

	if(ckAssertAttributeReturnValueOK(env, (*rv), __FUNCTION__, ckAttributesLength) != CK_ASSERT_OK) {
		TRACE0(tag_call, __FUNCTION__, "exiting ");
		return EXIT_FAILURE;
	}


	TRACE0(tag_call, __FUNCTION__, "exiting ");
	return EXIT_SUCCESS;
}

/*
 * function to free all memory allocated for the values of the given attribute array
 *
 * @param ckpAttributes - the attribute array
 * @param length - the length of the attribute array
 */
void freeAttributeArray(CK_ATTRIBUTE_PTR *ckpAttributes, CK_ULONG length, CK_BBOOL freeInnerArray){
	CK_ULONG i = 0;
	TRACE0(tag_call, __FUNCTION__, "entering ");

	if(*ckpAttributes != NULL_PTR){
		for (i = 0; i < length; i++) {
			freeAttributeValue(*ckpAttributes, i, freeInnerArray);
		}
		free(*ckpAttributes);
		*ckpAttributes = NULL_PTR;
	}
	TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * function to free all memory allocated for one given attribute in an attribute array
 *
 * @param ckpAttributes - the attribute array
 * @param length - the index of the attribute that shall be freed
 */
void freeAttributeValue(CK_ATTRIBUTE_PTR ckpAttributes, CK_ULONG ind, CK_BBOOL freeInnerArray){
	CK_ULONG j = 0;
	signed long signedLength;
	CK_ULONG length = 0;
	CK_ATTRIBUTE_PTR arrayAttribute;

	TRACE0(tag_call, __FUNCTION__, "entering ");
	if (ckpAttributes[ind].pValue != NULL_PTR) {
		TRACE1(tag_call, __FUNCTION__, "free %d. element in attribute array", (unsigned int)ind);
		// cka_allowed_mechanisms is not an attribute array but a mechanism array
		// if ((ckpAttributes[ind].type & CKF_ARRAY_ATTRIBUTE) == CKF_ARRAY_ATTRIBUTE){
		if (((ckpAttributes[ind].type == CKA_WRAP_TEMPLATE) || (ckpAttributes[ind].type == CKA_UNWRAP_TEMPLATE)) && freeInnerArray == CK_TRUE){
			arrayAttribute = (CK_ATTRIBUTE_PTR)ckpAttributes[ind].pValue;
			signedLength = ckpAttributes[ind].ulValueLen;
			if(signedLength > 0){
				length = signedLength/sizeof(CK_ATTRIBUTE);
				for (j=0; j<length; j++){
					if (arrayAttribute[j].pValue != NULL_PTR) {
						TRACE1(tag_call, __FUNCTION__, "free %d. element in array attribute", (unsigned int)j);
						free(arrayAttribute[j].pValue);
						arrayAttribute[j].pValue = NULL_PTR;
						arrayAttribute[j].ulValueLen = 0;
					}
				}
			}
		}
		free(ckpAttributes[ind].pValue);
		ckpAttributes[ind].pValue = NULL_PTR;
        ckpAttributes[ind].ulValueLen = 0;
	}
	TRACE0(tag_call, __FUNCTION__, "exiting ");
}



/*
 * Returns the required space to take the attribute's value according to the given attribute type
 *
 * @param type: The type of CK_ATTRIBUTE to be checked
 * @return - The required space or an estimation for the attribute's value
 */
CK_ULONG getRequiredSpace(CK_ATTRIBUTE_TYPE type) {

	switch (type) {

	case CKA_CLASS:
		return sizeof(CK_OBJECT_CLASS); /* defacto a CK_ULONG */
		break;
	case CKA_KEY_TYPE:
		return sizeof(CK_KEY_TYPE); /* defacto a CK_ULONG */
		break;
	case CKA_CERTIFICATE_TYPE:
		return sizeof(CK_CERTIFICATE_TYPE); /* defacto a CK_ULONG */
		break;
	case CKA_HW_FEATURE_TYPE:
		return sizeof(CK_HW_FEATURE_TYPE); /* defacto a CK_ULONG */
		break;
	case CKA_KEY_GEN_MECHANISM:
	case CKA_MECHANISM_TYPE:
		return sizeof(CK_MECHANISM_TYPE); /* defacto a CK_ULONG */
		break;
	case CKA_MODULUS_BITS:
	case CKA_VALUE_BITS:
	case CKA_VALUE_LEN:
	case CKA_PRIME_BITS:
	case CKA_SUBPRIME_BITS:
	case CKA_CERTIFICATE_CATEGORY:
	case CKA_JAVA_MIDP_SECURITY_DOMAIN:
	case CKA_AUTH_PIN_FLAGS:
	case CKA_PIXEL_X:
	case CKA_PIXEL_Y:
	case CKA_RESOLUTION:
	case CKA_CHAR_ROWS:
	case CKA_CHAR_COLUMNS:
	case CKA_BITS_PER_PIXEL:
		return sizeof(CK_ULONG);
		break;

		/* can be CK_BYTE[],CK_CHAR[] or big integer*/
	case CKA_VALUE:
	case CKA_OBJECT_ID:
	case CKA_SUBJECT:
	case CKA_ID:
	case CKA_ISSUER:
	case CKA_SERIAL_NUMBER:
	case CKA_OWNER:
	case CKA_AC_ISSUER:
	case CKA_ATTR_TYPES:
	case CKA_ECDSA_PARAMS:
		/* CKA_EC_PARAMS is the same, these two are equivalent */
	case CKA_EC_POINT:
	case CKA_PRIVATE_EXPONENT:
	case CKA_PRIME_1:
	case CKA_PRIME_2:
	case CKA_EXPONENT_1:
	case CKA_EXPONENT_2:
	case CKA_COEFFICIENT:
	case CKA_CHECK_VALUE:
	case CKA_HASH_OF_SUBJECT_PUBLIC_KEY:
	case CKA_HASH_OF_ISSUER_PUBLIC_KEY:
	case CKA_MODULUS:
	case CKA_PUBLIC_EXPONENT:
	case CKA_PRIME:
	case CKA_SUBPRIME:
	case CKA_BASE:
	case CKA_REQUIRED_CMS_ATTRIBUTES:
	case CKA_DEFAULT_CMS_ATTRIBUTES:
	case CKA_SUPPORTED_CMS_ATTRIBUTES:
	case CKA_VENDOR_DEFINED:
		/*byte array*/
		return attributeLengthBadCase;
		break;

	case CKA_RESET_ON_INIT:
	case CKA_HAS_RESET:
	case CKA_TOKEN:
	case CKA_PRIVATE:
	case CKA_MODIFIABLE:
	case CKA_DERIVE:
	case CKA_LOCAL:
	case CKA_ENCRYPT:
	case CKA_VERIFY:
	case CKA_VERIFY_RECOVER:
	case CKA_WRAP:
	case CKA_SENSITIVE:
	case CKA_SECONDARY_AUTH:
	case CKA_DECRYPT:
	case CKA_SIGN:
	case CKA_SIGN_RECOVER:
	case CKA_UNWRAP:
	case CKA_EXTRACTABLE:
	case CKA_ALWAYS_SENSITIVE:
	case CKA_NEVER_EXTRACTABLE:
	case CKA_TRUSTED:
	case CKA_WRAP_WITH_TRUSTED:
	case CKA_ALWAYS_AUTHENTICATE:
	case CKA_COLOR:
		return sizeof(CK_BBOOL);
		break;

	case CKA_LABEL:
	case CKA_APPLICATION:
	case CKA_URL:
	case CKA_CHAR_SETS:
	case CKA_ENCODING_METHODS:
	case CKA_MIME_TYPES:
		/*char array*/
		return attributeLengthBadCase;
		break;

	case CKA_START_DATE:
	case CKA_END_DATE:
		return sizeof(CK_DATE);
		break;

	case CKA_ALLOWED_MECHANISMS:
		return attributeNumberBadCase*sizeof(CK_MECHANISM_TYPE);
		break;

	case CKA_UNWRAP_TEMPLATE:
	case CKA_WRAP_TEMPLATE:
		return attributeNumberBadCase*sizeof(CK_ATTRIBUTE);
		break;

	default:
		return attributeLengthBadCase;
		break;
	}
}

int preAllocateAttributeArrayValues(JNIEnv *env, const char* callerMethodName, CK_ATTRIBUTE_PTR ckpAttributes, CK_ULONG ckAttributesLength,
		CK_ATTRIBUTE_PTR arrayAttributes, CK_ULONG arrayAttributesLength){
	CK_ULONG i, j, k, length, valueLength;
	CK_ULONG ckBufferLength;
	CK_ATTRIBUTE_PTR arrayAttribute;
	CK_ULONG arrayAttributeIndex = 0;

	for (i = 0; i < ckAttributesLength; i++) {
		TRACE1(tag_debug, __FUNCTION__,"get required byte length for attribute type 0x%X", (unsigned int)(ckpAttributes[i].type));
		valueLength = getRequiredSpace(ckpAttributes[i].type);

		// allocate array
		ckBufferLength = sizeof(CK_BYTE) * valueLength;
		TRACE1(tag_call, __FUNCTION__, "allocating %d bytes", (unsigned int)ckBufferLength);

		// cka_allowed_mechanisms is not an attribute array but a mechanism array
		// if ((ckpAttributes[i].type & CKF_ARRAY_ATTRIBUTE) == CKF_ARRAY_ATTRIBUTE){
		if ((ckpAttributes[i].type == CKA_WRAP_TEMPLATE) || (ckpAttributes[i].type == CKA_UNWRAP_TEMPLATE)){
			ckpAttributes[i].pValue = (CK_ATTRIBUTE_PTR) malloc(ckBufferLength);
		}else{
			ckpAttributes[i].pValue = (void *) malloc(ckBufferLength);
		}
		ckpAttributes[i].ulValueLen = ckBufferLength;

		// clean up if array could not be allocated
		if (ckpAttributes[i].pValue == NULL_PTR && ckBufferLength!=0) {
			TRACE0(tag_call, __FUNCTION__, "exiting ");
			return EXIT_MEM_FAILURE;
		}
		// cka_allowed_mechanisms is not an attribute array but a mechanism array
		// if ((ckpAttributes[i].type & CKF_ARRAY_ATTRIBUTE) == CKF_ARRAY_ATTRIBUTE){
		if ((ckpAttributes[i].type == CKA_WRAP_TEMPLATE) || (ckpAttributes[i].type == CKA_UNWRAP_TEMPLATE)){
			// initialize array attribute values
			arrayAttribute = (CK_ATTRIBUTE_PTR)ckpAttributes[i].pValue;
			length = ckpAttributes[i].ulValueLen/sizeof(CK_ATTRIBUTE);
			if(ckpAttributes[i].type == CKA_UNWRAP_TEMPLATE || ckpAttributes[i].type == CKA_WRAP_TEMPLATE)
				valueLength = sizeof(CK_BBOOL);
			else if(ckpAttributes[i].type == CKA_ALLOWED_MECHANISMS)
				valueLength = sizeof(CK_MECHANISM_TYPE);
			else
				valueLength = attributeLengthBadCase;
			TRACE2(tag_call, __FUNCTION__, "allocating %d bytes for each of the %d attributes in the attribute array", (unsigned int)valueLength, (unsigned int)length);
			for (j=0; j<length; j++){
				arrayAttribute[j].pValue = (CK_ATTRIBUTE_PTR) malloc(valueLength);
				arrayAttribute[j].ulValueLen = valueLength;

				// clean up if array could not be allocated
				if (arrayAttribute[j].pValue == NULL_PTR && valueLength!=0) {
					// free all inner pointers allocated so far, as only pointers in pointers-copy-array (arrayAttributes) will be freed
					for (k=0; k<j; k++){
						free(arrayAttribute[k].pValue);
						arrayAttribute[k].pValue = NULL_PTR;
					}
					TRACE0(tag_call, __FUNCTION__, "exiting ");
					return EXIT_MEM_FAILURE;
				}
			}
			if(arrayAttributeIndex < arrayAttributesLength){
				TRACE1(tag_call, __FUNCTION__, "copy %d. array attribute", (unsigned int)arrayAttributeIndex);
				arrayAttributes[arrayAttributeIndex].type = ckpAttributes[i].type;
				arrayAttributes[arrayAttributeIndex].pValue = (CK_ATTRIBUTE_PTR) malloc(ckBufferLength);
				if (arrayAttributes[arrayAttributeIndex].pValue == NULL_PTR && ckBufferLength!=0) {
					// free inner pointers of this attribute, as only pointers in pointers-copy-array (arrayAttributes) will be freed
					freeAttributeValue(ckpAttributes, i, CK_TRUE);
					TRACE0(tag_call, __FUNCTION__, "exiting ");
					return EXIT_MEM_FAILURE;
				}
				arrayAttributes[arrayAttributeIndex].ulValueLen = ckBufferLength;
				memcpy(arrayAttributes[arrayAttributeIndex].pValue, arrayAttribute, ckBufferLength);
			}else{
				return throwException(env, CKR_ARGUMENTS_BAD, callerMethodName);
			}
			arrayAttributeIndex++;
		}
	}
	return EXIT_SUCCESS;
}

void checkArrayAttributePointers(CK_ATTRIBUTE_PTR currentAttribute, CK_ATTRIBUTE_PTR attributeCopy){
	CK_ATTRIBUTE_PTR attributes, attributesCopies;

	TRACE0(tag_call, __FUNCTION__, "entering ");
	attributes = (CK_ATTRIBUTE_PTR)currentAttribute->pValue;
	attributesCopies = (CK_ATTRIBUTE_PTR)attributeCopy->pValue;

	// if first element is incorrect, module doesn't support array attributes
	// and only stored pointers instead of actual values;
	// correct pointers overwritten by invalid pointer addresses;
	// set whole attribute to null
	if(attributes[0].pValue != NULL_PTR && attributes[0].pValue != attributesCopies[0].pValue){
		TRACE0(tag_call, __FUNCTION__, "pointers differ");
		free(currentAttribute->pValue);
		currentAttribute->pValue = NULL_PTR;
		currentAttribute->ulValueLen = 0;
	}

	TRACE0(tag_call, __FUNCTION__, "exiting ");
}

