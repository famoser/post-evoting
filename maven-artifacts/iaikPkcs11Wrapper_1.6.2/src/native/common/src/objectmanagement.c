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

/* ************************************************************************** */
/* The native implementation of the methods of the PKCS11Implementation class */
/* for creating, destroying, changing or retrieving objects                   */
/* ************************************************************************** */

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_CreateObject
 * Signature: (J[Liaik/pkcs/pkcs11/wrapper/CK_ATTRIBUTE;Z)J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobjectArray jTemplate      CK_ATTRIBUTE_PTR pTemplate
 *                                      CK_ULONG ulCount
 * @return  jlong jObjectHandle         CK_OBJECT_HANDLE_PTR phObject
 */
JNIEXPORT jlong JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1CreateObject
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobjectArray jTemplate, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_OBJECT_HANDLE ckObjectHandle;
    CK_ATTRIBUTE_PTR ckpAttributes = NULL_PTR, ckAttributeArray;
    CK_ULONG ckAttributesLength;
    jlong jObjectHandle;
    CK_ULONG i, j, length;
    CK_RV rv;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;

    TRACE0(tag_call, __FUNCTION__, "entering");

    moduleData = getModuleEntry(env, obj);
    if (moduleData == NULL_PTR) {
	throwDisconnectedRuntimeException(env);
	return 0L;
    }
    ckpFunctions = getFunctionList(env, moduleData);
    if (ckpFunctions == NULL_PTR) {
	return 0L;
    }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    if (jAttributeArrayToCKAttributeArray(env, jTemplate, &ckpAttributes, &ckAttributesLength, jUseUtf8)) {
	return 0L;
    }

    rv = (*ckpFunctions->C_CreateObject) (ckSessionHandle, ckpAttributes, ckAttributesLength, &ckObjectHandle);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jObjectHandle = ckULongToJLong(ckObjectHandle);
    else
	jObjectHandle = 0L;

    for (i = 0; i < ckAttributesLength; i++)
	if (ckpAttributes[i].pValue != NULL_PTR) {
	    if ((ckpAttributes[i].type == 0x40000211) || (ckpAttributes[i].type == 0x40000212)) {
		ckAttributeArray = (CK_ATTRIBUTE_PTR) ckpAttributes[i].pValue;
		length = ckpAttributes[i].ulValueLen / sizeof(CK_ATTRIBUTE);
		for (j = 0; j < length; j++) {
		    free(ckAttributeArray[j].pValue);
		}
	    }
	    free(ckpAttributes[i].pValue);
	}
    free(ckpAttributes);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jObjectHandle;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_CopyObject
 * Signature: (JJ[Liaik/pkcs/pkcs11/wrapper/CK_ATTRIBUTE;Z)J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jlong jObjectHandle         CK_OBJECT_HANDLE hObject
 * @param   jobjectArray jTemplate      CK_ATTRIBUTE_PTR pTemplate
 *                                      CK_ULONG ulCount
 * @return  jlong jNewObjectHandle      CK_OBJECT_HANDLE_PTR phNewObject
 */
JNIEXPORT jlong JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1CopyObject
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jlong jObjectHandle, jobjectArray jTemplate, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_OBJECT_HANDLE ckObjectHandle;
    CK_OBJECT_HANDLE ckNewObjectHandle;
    CK_ATTRIBUTE_PTR ckpAttributes = NULL_PTR, ckAttributeArray;
    CK_ULONG ckAttributesLength;
    jlong jNewObjectHandle;
    CK_ULONG i, j, length;
    CK_RV rv;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;

    TRACE0(tag_call, __FUNCTION__, "entering");

    moduleData = getModuleEntry(env, obj);
    if (moduleData == NULL_PTR) {
	throwDisconnectedRuntimeException(env);
	return 0L;
    }
    ckpFunctions = getFunctionList(env, moduleData);
    if (ckpFunctions == NULL_PTR) {
	return 0L;
    }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckObjectHandle = jLongToCKULong(jObjectHandle);
    if (jAttributeArrayToCKAttributeArray(env, jTemplate, &ckpAttributes, &ckAttributesLength, jUseUtf8)) {
	return 0L;
    }

    rv = (*ckpFunctions->C_CopyObject) (ckSessionHandle, ckObjectHandle, ckpAttributes, ckAttributesLength,
					&ckNewObjectHandle);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jNewObjectHandle = ckULongToJLong(ckNewObjectHandle);
    else
	jNewObjectHandle = 0L;

    for (i = 0; i < ckAttributesLength; i++)
	if (ckpAttributes[i].pValue != NULL_PTR) {
	    if ((ckpAttributes[i].type == 0x40000211) || (ckpAttributes[i].type == 0x40000212)) {
		ckAttributeArray = (CK_ATTRIBUTE_PTR) ckpAttributes[i].pValue;
		length = ckpAttributes[i].ulValueLen / sizeof(CK_ATTRIBUTE);
		for (j = 0; j < length; j++) {
		    free(ckAttributeArray[j].pValue);
		}
	    }
	    free(ckpAttributes[i].pValue);
	}
    free(ckpAttributes);

    TRACE0(tag_call, __FUNCTION__, "exiting ");

    return jNewObjectHandle;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DestroyObject
 * Signature: (JJ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jlong jObjectHandle         CK_OBJECT_HANDLE hObject
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DestroyObject
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jlong jObjectHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_OBJECT_HANDLE ckObjectHandle;
    CK_RV rv;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;

    TRACE0(tag_call, __FUNCTION__, "entering");

    moduleData = getModuleEntry(env, obj);
    if (moduleData == NULL_PTR) {
	throwDisconnectedRuntimeException(env);
	return;
    }
    ckpFunctions = getFunctionList(env, moduleData);
    if (ckpFunctions == NULL_PTR) {
	return;
    }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckObjectHandle = jLongToCKULong(jObjectHandle);

    rv = (*ckpFunctions->C_DestroyObject) (ckSessionHandle, ckObjectHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetObjectSize
 * Signature: (JJ)J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jlong jObjectHandle         CK_OBJECT_HANDLE hObject
 * @return  jlong jObjectSize           CK_ULONG_PTR pulSize
 */
JNIEXPORT jlong JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetObjectSize
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jlong jObjectHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_OBJECT_HANDLE ckObjectHandle;
    CK_ULONG ckObjectSize;
    jlong jObjectSize;
    CK_RV rv;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;

    TRACE0(tag_call, __FUNCTION__, "entering");

    moduleData = getModuleEntry(env, obj);
    if (moduleData == NULL_PTR) {
	throwDisconnectedRuntimeException(env);
	return 0L;
    }
    ckpFunctions = getFunctionList(env, moduleData);
    if (ckpFunctions == NULL_PTR) {
	return 0L;
    }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckObjectHandle = jLongToCKULong(jObjectHandle);

    rv = (*ckpFunctions->C_GetObjectSize) (ckSessionHandle, ckObjectHandle, &ckObjectSize);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return 0L;
    }

    jObjectSize = ckULongToJLong(ckObjectSize);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jObjectSize;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_SetAttributeValue
 * Signature: (JJ[Liaik/pkcs/pkcs11/wrapper/CK_ATTRIBUTE;Z)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jlong jObjectHandle         CK_OBJECT_HANDLE hObject
 * @param   jobjectArray jTemplate      CK_ATTRIBUTE_PTR pTemplate
 *                                      CK_ULONG ulCount
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1SetAttributeValue
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jlong jObjectHandle, jobjectArray jTemplate, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_OBJECT_HANDLE ckObjectHandle;
    CK_ATTRIBUTE_PTR ckpAttributes = NULL_PTR, ckAttributeArray;
    CK_ULONG ckAttributesLength;
    CK_ULONG i, j, length;
    CK_RV rv;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;

    TRACE0(tag_call, __FUNCTION__, "entering");

    moduleData = getModuleEntry(env, obj);
    if (moduleData == NULL_PTR) {
	throwDisconnectedRuntimeException(env);
	return;
    }
    ckpFunctions = getFunctionList(env, moduleData);
    if (ckpFunctions == NULL_PTR) {
	return;
    }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckObjectHandle = jLongToCKULong(jObjectHandle);
    jAttributeArrayToCKAttributeArray(env, jTemplate, &ckpAttributes, &ckAttributesLength, jUseUtf8);

    rv = (*ckpFunctions->C_SetAttributeValue) (ckSessionHandle, ckObjectHandle, ckpAttributes, ckAttributesLength);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    for (i = 0; i < ckAttributesLength; i++) {
	if (ckpAttributes[i].pValue != NULL_PTR) {
	    if ((ckpAttributes[i].type == 0x40000211) || (ckpAttributes[i].type == 0x40000212)) {
		ckAttributeArray = (CK_ATTRIBUTE_PTR) ckpAttributes[i].pValue;
		length = ckpAttributes[i].ulValueLen / sizeof(CK_ATTRIBUTE);
		for (j = 0; j < length; j++) {
		    free(ckAttributeArray[j].pValue);
		}
	    }
	    free(ckpAttributes[i].pValue);
	}
    }
    free(ckpAttributes);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_FindObjectsInit
 * Signature: (J[Liaik/pkcs/pkcs11/wrapper/CK_ATTRIBUTE;Z)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobjectArray jTemplate      CK_ATTRIBUTE_PTR pTemplate
 *                                      CK_ULONG ulCount
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1FindObjectsInit
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobjectArray jTemplate, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_ATTRIBUTE_PTR ckpAttributes = NULL_PTR, ckAttributeArray;
    CK_ULONG ckAttributesLength;
    CK_ULONG i, j, length;
    CK_RV rv;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;

    TRACE0(tag_call, __FUNCTION__, "entering");

    moduleData = getModuleEntry(env, obj);
    if (moduleData == NULL_PTR) {
	throwDisconnectedRuntimeException(env);
	return;
    }
    ckpFunctions = getFunctionList(env, moduleData);
    if (ckpFunctions == NULL_PTR) {
	return;
    }

    TRACE2(tag_debug, __FUNCTION__, ", hSession=%d, pTemplate=%p", (int)jSessionHandle, jTemplate);

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    if (jAttributeArrayToCKAttributeArray(env, jTemplate, &ckpAttributes, &ckAttributesLength, jUseUtf8)) {
	return;
    }

    rv = (*ckpFunctions->C_FindObjectsInit) (ckSessionHandle, ckpAttributes, ckAttributesLength);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    for (i = 0; i < ckAttributesLength; i++) {
	if (ckpAttributes[i].pValue != NULL_PTR) {
	    if ((ckpAttributes[i].type == 0x40000211) || (ckpAttributes[i].type == 0x40000212)) {
		ckAttributeArray = (CK_ATTRIBUTE_PTR) ckpAttributes[i].pValue;
		length = ckpAttributes[i].ulValueLen / sizeof(CK_ATTRIBUTE);
		for (j = 0; j < length; j++) {
		    free(ckAttributeArray[j].pValue);
		}
	    }
	    free(ckpAttributes[i].pValue);
	}
    }
    free(ckpAttributes);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_FindObjects
 * Signature: (JJ)[J
 * Parametermapping:                        *PKCS11*
 * @param   jlong jSessionHandle            CK_SESSION_HANDLE hSession
 * @param   jlong jMaxObjectCount           CK_ULONG ulMaxObjectCount
 * @return  jlongArray jObjectHandleArray   CK_OBJECT_HANDLE_PTR phObject
 *                                          CK_ULONG_PTR pulObjectCount
 */
JNIEXPORT jlongArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1FindObjects
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jlong jMaxObjectCount) {
    CK_RV rv;
    CK_SESSION_HANDLE ckSessionHandle;
    CK_ULONG ckMaxObjectLength;
    CK_OBJECT_HANDLE_PTR ckpObjectHandleArray;
    CK_ULONG ckActualObjectCount;
    jlongArray jObjectHandleArray;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;

    TRACE0(tag_call, __FUNCTION__, "entering");

    moduleData = getModuleEntry(env, obj);
    if (moduleData == NULL_PTR) {
	throwDisconnectedRuntimeException(env);
	return NULL_PTR;
    }
    ckpFunctions = getFunctionList(env, moduleData);
    if (ckpFunctions == NULL_PTR) {
	return NULL_PTR;
    }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    ckMaxObjectLength = jLongToCKULong(jMaxObjectCount);
    ckpObjectHandleArray = (CK_OBJECT_HANDLE_PTR) malloc(sizeof(CK_OBJECT_HANDLE) * ckMaxObjectLength);
    if (ckpObjectHandleArray == NULL_PTR && ckMaxObjectLength != 0) {
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_FindObjects) (ckSessionHandle, ckpObjectHandleArray, ckMaxObjectLength,
					 &ckActualObjectCount);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK) {
	TRACE3(tag_debug, __FUNCTION__, "got ArrayHandle %p limited to %u entries having %u entries",
	       ckpObjectHandleArray, (unsigned int)ckMaxObjectLength, (unsigned int)ckActualObjectCount);
	jObjectHandleArray = ckULongArrayToJLongArray(env, ckpObjectHandleArray, ckActualObjectCount);
    } else
	jObjectHandleArray = NULL_PTR;

    free(ckpObjectHandleArray);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jObjectHandleArray;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_FindObjectsFinal
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1FindObjectsFinal
    (JNIEnv * env, jobject obj, jlong jSessionHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;

    TRACE0(tag_call, __FUNCTION__, "entering");

    moduleData = getModuleEntry(env, obj);
    if (moduleData == NULL_PTR) {
	throwDisconnectedRuntimeException(env);
	return;
    }
    ckpFunctions = getFunctionList(env, moduleData);
    if (ckpFunctions == NULL_PTR) {
	return;
    }

    ckSessionHandle = jLongToCKULong(jSessionHandle);
    rv = (*ckpFunctions->C_FindObjectsFinal) (ckSessionHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}
