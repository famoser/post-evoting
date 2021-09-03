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
/* Helper functions to support conversions between Java and Cryptoki types    */
/* ************************************************************************** */

/*
 * function to throw a Java PKCS#11Exception for the given PKCS#11 return value
 *
 * @param env - used to call JNI functions and to get the Exception class
 * @param returnValue - of the PKCS#11 function
 * @param callerMethodName - name of the caller-function
 */
jlong throwException(JNIEnv *env, CK_RV returnValue, const char* callerMethodName){
  jclass jPKCS11ExceptionClass;
  jmethodID jConstructor;
  jthrowable jPKCS11Exception;
  jlong jErrorCode;

  jPKCS11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
  assert(jPKCS11ExceptionClass != 0);
  jConstructor = (*env)->GetMethodID(env, jPKCS11ExceptionClass, "<init>", "(J)V");
  assert(jConstructor != 0);
  jErrorCode = ckULongToJLong(returnValue);
  jPKCS11Exception = (jthrowable) (*env)->NewObject(env, jPKCS11ExceptionClass, jConstructor, jErrorCode);
  (*env)->Throw(env, jPKCS11Exception);
  TRACE1(tag_error, callerMethodName, "got %u instead of CKR_OK, going to raise an exception", (unsigned int) returnValue);
  return jErrorCode ;
}

/*
 * the following functions convert Java arrays to PKCS#11 array pointers and
 * their array length and vice versa
 *
 * void j<Type>ArrayToCK<Type>Array(JNIEnv *env,
 *                                  const j<Type>Array jArray,
 *                                  CK_<Type>_PTR *ckpArray,
 *                                  CK_ULONG_PTR ckLength);
 *
 * j<Type>Array ck<Type>ArrayToJ<Type>Array(JNIEnv *env,
 *                                          const CK_<Type>_PTR ckpArray,
 *                                          CK_ULONG ckLength);
 *
 * PKCS#11 arrays consist always of a pointer to the beginning of the array and
 * the array length whereas Java arrays carry their array length.
 *
 * The Functions to convert a Java array to a PKCS#11 array are void functions.
 * Their arguments are the Java array object to convert, the reference to the
 * array pointer, where the new PKCS#11 array should be stored and the reference
 * to the array length where the PKCS#11 array length should be stored. These two
 * references must not be NULL_PTR.
 *
 * The functions first obtain the array length of the Java array and then allocate
 * the memory for the PKCS#11 array and set the array length. Then each element
 * gets converted depending on their type. After use the allocated memory of the
 * PKCS#11 array has to be explicitly freed.
 *
 * The Functions to convert a PKCS#11 array to a Java array get the PKCS#11 array
 * pointer and the array length and they return the new Java array object. The
 * Java array does not need to get freed after use.
 */

/*
 * converts a jbooleanArray to a CK_BBOOL array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array information
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_BBOOL array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 * @return 0 is successful
 */
int jBooleanArrayToCKBBoolArray(JNIEnv *env, const jbooleanArray jArray, CK_BBOOL **ckpArray, CK_ULONG_PTR ckpLength)
{
	jboolean* jpTemp;
	CK_ULONG i;

	if(jArray == NULL_PTR) {
		*ckpArray = NULL_PTR;
		*ckpLength = 0L;
		return 0;
	}
	*ckpLength = (*env)->GetArrayLength(env, jArray);
	jpTemp = (jboolean*) malloc((*ckpLength) * sizeof(jboolean));
  if (jpTemp == NULL_PTR && (*ckpLength)!=0) { *ckpArray = NULL_PTR; throwOutOfMemoryError(env); return 1; }
	(*env)->GetBooleanArrayRegion(env, jArray, 0, *ckpLength, jpTemp);
	*ckpArray = (CK_BBOOL*) malloc ((*ckpLength) * sizeof(CK_BBOOL));
  if (*ckpArray == NULL_PTR && (*ckpLength)!=0) { free(jpTemp); throwOutOfMemoryError(env); return 2; }
	for (i=0; i<(*ckpLength); i++) {
		(*ckpArray)[i] = jBooleanToCKBBool(jpTemp[i]);
	}
	free(jpTemp);
  return 0;
}

/*
 * converts a jbyteArray to a CK_BYTE array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array information
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_BYTE array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 * @return 0 is successful
 */
int jByteArrayToCKByteArray(JNIEnv *env, const jbyteArray jArray, CK_BYTE_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
	jbyte* jpTemp;
	CK_ULONG i;

	if(jArray == NULL_PTR) {
		*ckpArray = NULL_PTR;
		*ckpLength = 0L;
		return 0;
	}
	*ckpLength = (*env)->GetArrayLength(env, jArray);
	if(*ckpLength == 0L) {
		*ckpArray = NULL_PTR;
		return 0;
	}
	jpTemp = (jbyte*) malloc((*ckpLength) * sizeof(jbyte));
  if (jpTemp == NULL_PTR && (*ckpLength)!=0) { *ckpArray = NULL_PTR; throwOutOfMemoryError(env); return 1; }
	(*env)->GetByteArrayRegion(env, jArray, 0, *ckpLength, jpTemp);

  /* if CK_BYTE is the same size as jbyte, we save an additional copy */
  if (sizeof(CK_BYTE) == sizeof(jbyte)) {
    *ckpArray = (CK_BYTE_PTR) jpTemp;
  } else {
	  *ckpArray = (CK_BYTE_PTR) malloc ((*ckpLength) * sizeof(CK_BYTE));
    if (*ckpArray == NULL_PTR && (*ckpLength)!=0) { free(jpTemp); throwOutOfMemoryError(env); return 2; }
	  for (i=0; i<(*ckpLength); i++) {
		  (*ckpArray)[i] = jByteToCKByte(jpTemp[i]);
	  }
	  free(jpTemp);
  }
  return 0;
}

/*
 * converts a jlongArray to a CK_ULONG array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array information
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_ULONG array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 * @return 0 is successful
 */
int jLongArrayToCKULongArray(JNIEnv *env, const jlongArray jArray, CK_ULONG_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
	jlong* jpTemp;
	CK_ULONG i;

	if(jArray == NULL_PTR) {
		*ckpArray = NULL_PTR;
		*ckpLength = 0L;
		return 0;
	}
	*ckpLength = (*env)->GetArrayLength(env, jArray);
	jpTemp = (jlong*) malloc((*ckpLength) * sizeof(jlong));
  if (jpTemp == NULL_PTR && (*ckpLength)!=0) { *ckpArray = NULL_PTR; throwOutOfMemoryError(env); return 1; }
	(*env)->GetLongArrayRegion(env, jArray, 0, *ckpLength, jpTemp);
	*ckpArray = (CK_ULONG_PTR) malloc (*ckpLength * sizeof(CK_ULONG));
  if (*ckpArray == NULL_PTR && (*ckpLength)!=0) { free(jpTemp); throwOutOfMemoryError(env); return 2; }
	for (i=0; i<(*ckpLength); i++) {
		(*ckpArray)[i] = jLongToCKULong(jpTemp[i]);
	}
	free(jpTemp);
	return 0;
}

/*
 * converts a jcharArray to a CK_CHAR array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array information
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_CHAR array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 * @return 0 is successful
 */
int jCharArrayToCKCharArray(JNIEnv *env, const jcharArray jArray, CK_CHAR_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{

	jchar* jpTemp;
	CK_ULONG i;

	if(jArray == NULL_PTR) {
		*ckpArray = NULL_PTR;
		*ckpLength = 0L;
		return 0;
	}

	TRACE0(tag_call, __FUNCTION__, "entering");
	*ckpLength = (*env)->GetArrayLength(env, jArray);
	jpTemp = (jchar*) malloc((*ckpLength) * sizeof(jchar));
  if (jpTemp == NULL_PTR && (*ckpLength)!=0) { *ckpArray = NULL_PTR; throwOutOfMemoryError(env); return 1; }
	(*env)->GetCharArrayRegion(env, jArray, 0, *ckpLength, jpTemp);
	*ckpArray = (CK_CHAR_PTR) malloc (*ckpLength * sizeof(CK_CHAR));
  if (*ckpArray == NULL_PTR && (*ckpLength)!=0) { free(jpTemp); throwOutOfMemoryError(env); return 2; }
	for (i=0; i<(*ckpLength); i++) {
		(*ckpArray)[i] = jCharToCKChar(jpTemp[i]);
	}
	free(jpTemp);
	TRACE0(tag_call, __FUNCTION__, "exiting ");
  return 0;
}

/*
 * converts a jcharArray to a CK_UTF8CHAR array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array information
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_UTF8CHAR array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 * @return 0 is successful
 */
int jCharArrayToCKUTF8CharArray(JNIEnv *env, const jcharArray jArray, CK_UTF8CHAR_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
	jbyte* jpTemp;
	CK_ULONG i;
	jclass jStringEncoderClass;
	jmethodID jEncoderMethod;
	jbyteArray jValue;

	TRACE0(tag_call, __FUNCTION__, "entering");
	if(jArray == NULL_PTR) {
		*ckpArray = NULL_PTR;
		*ckpLength = 0L;
		return 0;
	}
	jStringEncoderClass = (*env)->FindClass(env, CLASS_PKCS11UTIL);
	assert(jStringEncoderClass != 0);
	jEncoderMethod = (*env)->GetStaticMethodID(env, jStringEncoderClass, METHOD_ENCODER, "([C)[B");
	assert(jEncoderMethod != 0);
	jValue = (*env)->CallStaticObjectMethod(env, jStringEncoderClass, jEncoderMethod, jArray);
	if(jValue == 0)
		return 1;
	*ckpLength = (*env)->GetArrayLength(env, jValue);
	jpTemp = (jbyte*) malloc((*ckpLength) * sizeof(jbyte));
  if (jpTemp == NULL_PTR && (*ckpLength)!=0) { *ckpArray = NULL_PTR; throwOutOfMemoryError(env); return 1; }
	(*env)->GetByteArrayRegion(env, jValue, 0, *ckpLength, jpTemp);
	*ckpArray = (CK_UTF8CHAR_PTR) malloc (*ckpLength * sizeof(CK_UTF8CHAR));
  if (*ckpArray == NULL_PTR && (*ckpLength)!=0) { free(jpTemp); throwOutOfMemoryError(env); return 2; }
	for (i=0; i<(*ckpLength); i++) {
		(*ckpArray)[i] = jByteToCKUTF8Char(jpTemp[i]);
	}
	free(jpTemp);
	TRACE0(tag_call, __FUNCTION__, "exiting ");
  return 0;
}

/*
 * converts a jstring to a CK_CHAR array. The allocated memory has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array information
 * @param jArray - the Java array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_CHAR array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 * @return 0 is successful
 */
int jStringToCKUTF8CharArray(JNIEnv *env, const jstring jArray, CK_UTF8CHAR_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
	const char* pCharArray;
	jboolean isCopy;

	if(jArray == NULL_PTR) {
		*ckpArray = NULL_PTR;
		*ckpLength = 0L;
		return 0;
	}

	pCharArray = (*env)->GetStringUTFChars(env, jArray, &isCopy);
	*ckpLength = strlen(pCharArray);
	*ckpArray = (CK_UTF8CHAR_PTR) malloc((*ckpLength + 1) * sizeof(CK_UTF8CHAR));
  if (*ckpArray == NULL_PTR && (*ckpLength + 1)!=0) { throwOutOfMemoryError(env); return 1; }
	strcpy((char *) *ckpArray, pCharArray);
	(*env)->ReleaseStringUTFChars(env, (jstring) jArray, pCharArray);
  return 0;
}

/*
 * converts a jobjectArray with Java Attributes to a CK_ATTRIBUTE array. The allocated memory
 * has to be freed after use!
 *
 * @param env - used to call JNI functions to get the array information
 * @param jArray - the Java Attribute array (template) to convert
 * @param ckpArray - the reference, where the pointer to the new CK_ATTRIBUTE array will be
 *                   stored
 * @param ckpLength - the reference, where the array length will be stored
 * @return 0 is successful
 */
int jAttributeArrayToCKAttributeArray(JNIEnv *env, jobjectArray jArray, CK_ATTRIBUTE_PTR *ckpArray, CK_ULONG_PTR ckpLength, jboolean jUseUtf8)
{
	CK_ULONG i;
	jlong jLength;
	jobject jAttribute;

	TRACE0(tag_call, __FUNCTION__,"entering");
	if (jArray == NULL_PTR) {
		*ckpArray = NULL_PTR;
		*ckpLength = 0L;
	  TRACE0(tag_call, __FUNCTION__, "exiting ");
		return 0;
	}
	jLength = (*env)->GetArrayLength(env, jArray);
	*ckpLength = jLongToCKULong(jLength);
	TRACE1(tag_debug, __FUNCTION__, "array length is %u", (unsigned int)*ckpLength)
	*ckpArray = (CK_ATTRIBUTE_PTR) malloc(*ckpLength * sizeof(CK_ATTRIBUTE));
  if (*ckpArray == NULL_PTR && (*ckpLength)!=0) { throwOutOfMemoryError(env); return 1; }
	TRACE1(tag_debug, __FUNCTION__,"converting %d attributes", (int)jLength);
	for (i=0; i<(*ckpLength); i++) {
		TRACE1(tag_debug, __FUNCTION__,", getting %u. attribute", (unsigned int)i);
		jAttribute = (*env)->GetObjectArrayElement(env, jArray, i);
		TRACE1(tag_debug, __FUNCTION__,", converting %u. attribute", (unsigned int)i);
		(*ckpArray)[i] = jAttributeToCKAttribute(env, jAttribute, jUseUtf8);
	}
	TRACE0(tag_debug, __FUNCTION__,"Converted template with following types: ");
	for (i=0; i<(*ckpLength); i++) {
		TRACE1(tag_debug, __FUNCTION__,"0x%X", (unsigned int)(*ckpArray)[i].type);
	}
  TRACE0(tag_call, __FUNCTION__, "exiting ");
  return 0;
}

/*
 * converts a jobjectArray to a CK_VOID_PTR array. The allocated memory has to be freed after
 * use!
 * NOTE: this function does not work and is not used yet
 *
 * @param env - used to call JNI functions to get the array information
 * @param jArray - the Java object array to convert
 * @param ckpArray - the reference, where the pointer to the new CK_VOID_PTR array will be stored
 * @param ckpLength - the reference, where the array length will be stored
 * @return 0 is successful
 */
/*
int jObjectArrayToCKVoidPtrArray(JNIEnv *env, const jobjectArray jArray, CK_VOID_PTR_PTR *ckpArray, CK_ULONG_PTR ckpLength)
{
	jobject jTemp;
	CK_ULONG i;

	if(jArray == NULL_PTR) {
		*ckpArray = NULL_PTR;
		*ckpLength = 0L;
		return 0;
	}
	*ckpLength = (*env)->GetArrayLength(env, jArray);
	*ckpArray = (CK_VOID_PTR_PTR) malloc (*ckpLength * sizeof(CK_VOID_PTR));
  if (*ckpArray == NULL_PTR && (*ckpLength)!=0) { throwOutOfMemoryError(env); return 1; }
	for (i=0; i<(*ckpLength); i++) {
		jTemp = (*env)->GetObjectArrayElement(env, jArray, i);
		(*ckpArray)[i] = jObjectToCKVoidPtr(jTemp);
	}
	free(jTemp);
  return 0;
}
*/

/*
 * converts a CK_BYTE array and its length to a jbyteArray.
 *
 * @param env - used to call JNI functions to create the new Java array
 * @param ckpArray - the pointer to the CK_BYTE array to convert
 * @param ckpLength - the length of the array to convert
 * @return - the new Java byte array
 */
jbyteArray ckByteArrayToJByteArray(JNIEnv *env, const CK_BYTE_PTR ckpArray, CK_ULONG ckLength)
{
	CK_ULONG i;
	jbyte* jpTemp;
	jbyteArray jArray;
    TRACE0(tag_call, __FUNCTION__, "entering");

  /* if CK_BYTE is the same size as jbyte, we save an additional copy */
  if (sizeof(CK_BYTE) == sizeof(jbyte)) {
    jpTemp = (jbyte*) ckpArray;
  } else {
	  jpTemp = (jbyte*) malloc((ckLength) * sizeof(jbyte));
    if (jpTemp == NULL_PTR && ckLength!=0) { throwOutOfMemoryError(env); return NULL_PTR; }
	  for (i=0; i<ckLength; i++) {
		  jpTemp[i] = ckByteToJByte(ckpArray[i]);
	  }
  }

	jArray = (*env)->NewByteArray(env, ckULongToJSize(ckLength));
	(*env)->SetByteArrayRegion(env, jArray, 0, ckULongToJSize(ckLength), jpTemp);

  if (sizeof(CK_BYTE) != sizeof(jbyte)) {
    free(jpTemp);
  }

    TRACE0(tag_call, __FUNCTION__, "exiting");
	return jArray ;
}

/*
 * converts a CK_ULONG array and its length to a jlongArray.
 *
 * @param env - used to call JNI functions to create the new Java array
 * @param ckpArray - the pointer to the CK_ULONG array to convert
 * @param ckpLength - the length of the array to convert
 * @return - the new Java long array
 */
jlongArray ckULongArrayToJLongArray(JNIEnv *env, const CK_ULONG_PTR ckpArray, CK_ULONG ckLength)
{
	CK_ULONG i;
	jlong* jpTemp;
	jlongArray jArray;

	jpTemp = (jlong*) malloc((ckLength) * sizeof(jlong));
  if (jpTemp == NULL_PTR && ckLength!=0) { throwOutOfMemoryError(env); return NULL_PTR; }
	for (i=0; i<ckLength; i++) {
		jpTemp[i] = ckLongToJLong(ckpArray[i]);
	}
	jArray = (*env)->NewLongArray(env, ckULongToJSize(ckLength));
	(*env)->SetLongArrayRegion(env, jArray, 0, ckULongToJSize(ckLength), jpTemp);
	free(jpTemp);

	return jArray ;
}

/*
 * converts a CK_CHAR array and its length to a jcharArray.
 *
 * @param env - used to call JNI functions to create the new Java array
 * @param ckpArray - the pointer to the CK_CHAR array to convert
 * @param ckpLength - the length of the array to convert
 * @return - the new Java char array
 */
jcharArray ckCharArrayToJCharArray(JNIEnv *env, const CK_CHAR_PTR ckpArray, CK_ULONG ckLength)
{
	CK_ULONG i;
	jchar* jpTemp;
	jcharArray jArray;

	TRACE0(tag_call, __FUNCTION__, "entering");
	jpTemp = (jchar*) malloc(ckLength * sizeof(jchar));
  if (jpTemp == NULL_PTR && ckLength!=0) { throwOutOfMemoryError(env); return NULL_PTR; }
	for (i=0; i<ckLength; i++) {
		jpTemp[i] = ckCharToJChar(ckpArray[i]);
	}
	jArray = (*env)->NewCharArray(env, ckULongToJSize(ckLength));
	(*env)->SetCharArrayRegion(env, jArray, 0, ckULongToJSize(ckLength), jpTemp);
	free(jpTemp);

	TRACE0(tag_call, __FUNCTION__, "exiting");
	return jArray ;
}

/*
 * converts a CK_UTF8CHAR array and its length to a jcharArray.
 *
 * @param env - used to call JNI functions to create the new Java array
 * @param ckpArray - the pointer to the CK_UTF8CHAR array to convert
 * @param ckpLength - the length of the array to convert
 * @return - the new Java char array
 */
jcharArray ckUTF8CharArrayToJCharArray(JNIEnv *env, const CK_UTF8CHAR_PTR ckpArray, CK_ULONG ckLength)
{

	CK_ULONG i;
	jbyte* jpTemp;
	jbyteArray jArray;
	jclass jStringDecoderClass;
	jmethodID jDecoderMethod;
	jcharArray jValue;

	TRACE0(tag_call, __FUNCTION__, "entering");
	jpTemp = (jbyte*) malloc(ckLength * sizeof(jbyte));
  if (jpTemp == NULL_PTR && ckLength!=0) { throwOutOfMemoryError(env); return NULL_PTR; }
	for (i=0; i<ckLength; i++) {
		jpTemp[i] = ckUTF8CharToJByte(ckpArray[i]);
	}
	jArray = (*env)->NewByteArray(env, ckULongToJSize(ckLength));
	(*env)->SetByteArrayRegion(env, jArray, 0, ckULongToJSize(ckLength), jpTemp);

	jStringDecoderClass = (*env)->FindClass(env, CLASS_PKCS11UTIL);
	assert(jStringDecoderClass != 0);
	jDecoderMethod = (*env)->GetStaticMethodID(env, jStringDecoderClass, METHOD_DECODER, "([B)[C");
	assert(jDecoderMethod != 0);
	jValue = (*env)->CallStaticObjectMethod(env, jStringDecoderClass, jDecoderMethod, jArray);

	free(jpTemp);

	TRACE0(tag_call, __FUNCTION__, "exiting");
	return jValue ;
}

jobject ckAttributeArrayToJAttributeArray(JNIEnv *env, const CK_ATTRIBUTE_PTR ckpArray, CK_ULONG ckLength, jobject obj, jlong jSessionHandle, jlong jObjectHandle, jboolean jUseUtf8)
{
	jclass jAttributeClass;
	jobjectArray jAttributeArray;
	CK_ULONG i;
	CK_ULONG length;
	jobject jAttribute;
	jsize jlength;

	length = ckLength/sizeof(CK_ATTRIBUTE);
	jlength = ckULongToJSize(length);
	jAttributeClass = (*env)->FindClass(env, CLASS_ATTRIBUTE);
	assert(jAttributeClass != 0);
	/* allocate array, all elements NULL_PTR per default */
	jAttributeArray = (*env)->NewObjectArray(env, jlength, jAttributeClass, NULL_PTR);
	assert(jAttributeArray != 0);

	for (i=0; i<length; i++) {
		jAttribute = ckAttributePtrToJAttribute(env, &(ckpArray[i]), obj, jSessionHandle, jObjectHandle, jUseUtf8);
		(*env)->SetObjectArrayElement(env, jAttributeArray, i, jAttribute);
	}

	return jAttributeArray ;
}

/*
 * the following functions convert Java objects to PKCS#11 pointers and the
 * length in bytes and vice versa
 *
 * CK_<Type>_PTR j<Object>ToCK<Type>Ptr(JNIEnv *env, jobject jObject);
 *
 * jobject ck<Type>PtrToJ<Object>(JNIEnv *env, const CK_<Type>_PTR ckpValue);
 *
 * The functions that convert a Java object to a PKCS#11 pointer first allocate
 * the memory for the PKCS#11 pointer. Then they set each element corresponding
 * to the fields in the Java object to convert. After use the allocated memory of
 * the PKCS#11 pointer has to be explicitly freed.
 *
 * The functions to convert a PKCS#11 pointer to a Java object create a new Java
 * object first and than they set all fields in the object depending on the values
 * of the type or structure where the PKCS#11 pointer points to.
 */

/*
 * converts a CK_BBOOL pointer to a Java boolean Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpValue - the pointer to the CK_BBOOL value
 * @return - the new Java boolean object with the boolean value
 */
jobject ckBBoolPtrToJBooleanObject(JNIEnv *env, const CK_BBOOL *ckpValue)
{
	jclass jValueObjectClass;
	jmethodID jConstructor;
	jobject jValueObject;
	jboolean jValue;

	jValueObjectClass = (*env)->FindClass(env, "java/lang/Boolean");
	assert(jValueObjectClass != 0);
	jConstructor = (*env)->GetMethodID(env, jValueObjectClass, "<init>", "(Z)V");
	assert(jConstructor != 0);
	jValue = ckBBoolToJBoolean(*ckpValue);
	jValueObject = (*env)->NewObject(env, jValueObjectClass, jConstructor, jValue);
	assert(jValueObject != 0);

	return jValueObject ;
}

/*
 * converts a CK_ULONG pointer to a Java long Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpValue - the pointer to the CK_ULONG value
 * @return - the new Java long object with the long value
 */
jobject ckULongPtrToJLongObject(JNIEnv *env, const CK_ULONG_PTR ckpValue)
{
	jclass jValueObjectClass;
	jmethodID jConstructor;
	jobject jValueObject;
	jlong jValue;

	jValueObjectClass = (*env)->FindClass(env, "java/lang/Long");
	assert(jValueObjectClass != 0);
	jConstructor = (*env)->GetMethodID(env, jValueObjectClass, "<init>", "(J)V");
	assert(jConstructor != 0);
	jValue = ckULongToJLong(*ckpValue);
	jValueObject = (*env)->NewObject(env, jValueObjectClass, jConstructor, jValue);
	assert(jValueObject != 0);

	return jValueObject ;
}

/*
 * converts a pointer to a CK_DATE structure into a Java CK_DATE Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpValue - the pointer to the CK_DATE structure
 * @return - the new Java CK_DATE object
 */
jobject ckDatePtrToJDateObject(JNIEnv *env, const CK_DATE *ckpValue)
{
	jclass jValueObjectClass;
	jobject jValueObject;
	jcharArray jTempCharArray;
	jfieldID fieldID;

	/* load CK_DATE class */
	jValueObjectClass = (*env)->FindClass(env, CLASS_DATE);
	assert(jValueObjectClass != 0);
	/* create new CK_DATE jObject */
	jValueObject = (*env)->AllocObject(env, jValueObjectClass);
	assert(jValueObject != 0);

	/* set year */
	fieldID = (*env)->GetFieldID(env, jValueObjectClass, "year", "[C");
	assert(fieldID != 0);
	jTempCharArray = ckCharArrayToJCharArray(env, (CK_CHAR_PTR)(ckpValue->year), 4);
	(*env)->SetObjectField(env, jValueObject, fieldID, jTempCharArray);

	/* set month */
	fieldID = (*env)->GetFieldID(env, jValueObjectClass, "month", "[C");
	assert(fieldID != 0);
	jTempCharArray = ckCharArrayToJCharArray(env, (CK_CHAR_PTR)(ckpValue->month), 2);
	(*env)->SetObjectField(env, jValueObject, fieldID, jTempCharArray);

	/* set day */
	fieldID = (*env)->GetFieldID(env, jValueObjectClass, "day", "[C");
	assert(fieldID != 0);
	jTempCharArray = ckCharArrayToJCharArray(env, (CK_CHAR_PTR)(ckpValue->day), 2);
	(*env)->SetObjectField(env, jValueObject, fieldID, jTempCharArray);

	return jValueObject ;
}

/*
 * converts a pointer to a CK_VERSION structure into a Java CK_VERSION Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpVersion - the pointer to the CK_VERSION structure
 * @return - the new Java CK_VERSION object
 */
jobject ckVersionPtrToJVersion(JNIEnv *env, const CK_VERSION_PTR ckpVersion)
{
	jclass jVersionClass;
	jobject jVersionObject;
	jfieldID jFieldID;

	/* load CK_VERSION class */
	jVersionClass = (*env)->FindClass(env, CLASS_VERSION);
	assert(jVersionClass != 0);
	/* create new CK_VERSION object */
	jVersionObject = (*env)->AllocObject(env, jVersionClass);
	assert(jVersionObject != 0);
	/* set major */
	jFieldID = (*env)->GetFieldID(env, jVersionClass, "major", "B");
	assert(jFieldID != 0);
	(*env)->SetByteField(env, jVersionObject, jFieldID, (jbyte) (ckpVersion->major));
	/* set minor */
	jFieldID = (*env)->GetFieldID(env, jVersionClass, "minor", "B");
	assert(jFieldID != 0);
	(*env)->SetByteField(env, jVersionObject, jFieldID, (jbyte) (ckpVersion->minor));

	return jVersionObject ;
}

/*
 * converts a pointer to a CK_INFO structure into a Java CK_INFO Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpInfo - the pointer to the CK_INFO structure
 * @return - the new Java CK_INFO object
 */
jobject ckInfoPtrToJInfo(JNIEnv *env, const CK_INFO_PTR ckpInfo)
{
	jclass jInfoClass;
	jobject jInfoObject;
	jcharArray jTempCharArray;
	jfieldID jFieldID;
	jobject jTempVersion;

	/* load CK_INFO class */
	jInfoClass = (*env)->FindClass(env, CLASS_INFO);
	assert(jInfoClass != 0);
	/* create new CK_INFO object */
	jInfoObject = (*env)->AllocObject(env, jInfoClass);
	assert(jInfoObject != 0);

	/* set cryptokiVersion */
	jFieldID = (*env)->GetFieldID(env, jInfoClass, "cryptokiVersion", CLASS_NAME(CLASS_VERSION));
	assert(jFieldID != 0);
	jTempVersion = ckVersionPtrToJVersion(env, &(ckpInfo->cryptokiVersion));
	(*env)->SetObjectField(env, jInfoObject, jFieldID, jTempVersion);

	/* set manufacturerID */
	jFieldID = (*env)->GetFieldID(env, jInfoClass, "manufacturerID", "[C");
	assert(jFieldID != 0);
	jTempCharArray = ckUTF8CharArrayToJCharArray(env, &(ckpInfo->manufacturerID[0]), 32);
	(*env)->SetObjectField(env, jInfoObject, jFieldID, jTempCharArray);

	/* set flags */
	jFieldID = (*env)->GetFieldID(env, jInfoClass, "flags", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jInfoObject, jFieldID, ckULongToJLong(ckpInfo->flags));

	/* set libraryDescription */
	jFieldID = (*env)->GetFieldID(env, jInfoClass, "libraryDescription", "[C");
	assert(jFieldID != 0);
	jTempCharArray = ckUTF8CharArrayToJCharArray(env, &(ckpInfo->libraryDescription[0]) ,32);
	(*env)->SetObjectField(env, jInfoObject, jFieldID, jTempCharArray);

	/* set libraryVersion */
	jFieldID = (*env)->GetFieldID(env, jInfoClass, "libraryVersion", CLASS_NAME(CLASS_VERSION));
	assert(jFieldID != 0);
	jTempVersion = ckVersionPtrToJVersion(env, &(ckpInfo->libraryVersion));
	(*env)->SetObjectField(env, jInfoObject, jFieldID, jTempVersion);

	return jInfoObject ;
}

/*
 * converts a pointer to a CK_SLOT_INFO structure into a Java CK_SLOT_INFO Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpSlotInfo - the pointer to the CK_SLOT_INFO structure
 * @return - the new Java CK_SLOT_INFO object
 */
jobject ckSlotInfoPtrToJSlotInfo(JNIEnv *env, const CK_SLOT_INFO_PTR ckpSlotInfo)
{
	jclass jSlotInfoClass;
	jobject jSlotInfoObject;
	jcharArray jTempCharArray;
	jfieldID jFieldID;
	jobject jTempVersion;

	/* load CK_SLOT_INFO class */
	jSlotInfoClass = (*env)->FindClass(env, CLASS_SLOT_INFO);
	assert(jSlotInfoClass != 0);
	/* create new CK_SLOT_INFO object */
	jSlotInfoObject = (*env)->AllocObject(env, jSlotInfoClass);
	assert(jSlotInfoObject != 0);


	/* set slotDescription */
	jFieldID = (*env)->GetFieldID(env, jSlotInfoClass, "slotDescription", "[C");
	assert(jFieldID != 0);
	jTempCharArray = ckUTF8CharArrayToJCharArray(env, &(ckpSlotInfo->slotDescription[0]) ,64);
	(*env)->SetObjectField(env, jSlotInfoObject, jFieldID, jTempCharArray);

	/* set manufacturerID */
	jFieldID = (*env)->GetFieldID(env, jSlotInfoClass, "manufacturerID", "[C");
	assert(jFieldID != 0);
	jTempCharArray = ckUTF8CharArrayToJCharArray(env, &(ckpSlotInfo->manufacturerID[0]) ,32);
	(*env)->SetObjectField(env, jSlotInfoObject, jFieldID, jTempCharArray);

	/* set flags */
	jFieldID = (*env)->GetFieldID(env, jSlotInfoClass, "flags", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jSlotInfoObject, jFieldID, ckULongToJLong(ckpSlotInfo->flags));

	/* set hardwareVersion */
	jFieldID = (*env)->GetFieldID(env, jSlotInfoClass, "hardwareVersion", CLASS_NAME(CLASS_VERSION));
	assert(jFieldID != 0);
	jTempVersion = ckVersionPtrToJVersion(env, &(ckpSlotInfo->hardwareVersion));
	(*env)->SetObjectField(env, jSlotInfoObject, jFieldID, jTempVersion);

	/* set firmwareVersion */
	jFieldID = (*env)->GetFieldID(env, jSlotInfoClass, "firmwareVersion", CLASS_NAME(CLASS_VERSION));
	assert(jFieldID != 0);
	jTempVersion = ckVersionPtrToJVersion(env, &(ckpSlotInfo->firmwareVersion));
	(*env)->SetObjectField(env, jSlotInfoObject, jFieldID, jTempVersion);

	return jSlotInfoObject ;
}

/*
 * converts a pointer to a CK_TOKEN_INFO structure into a Java CK_TOKEN_INFO Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpTokenInfo - the pointer to the CK_TOKEN_INFO structure
 * @return - the new Java CK_TOKEN_INFO object
 */
jobject ckTokenInfoPtrToJTokenInfo(JNIEnv *env, const CK_TOKEN_INFO_PTR ckpTokenInfo)
{
	jclass jTokenInfoClass;
	jobject jTokenInfoObject;
	jcharArray jTempCharArray;
	jfieldID jFieldID;
	jobject jTempVersion;

	/* load CK_SLOT_INFO class */
	jTokenInfoClass = (*env)->FindClass(env, CLASS_TOKEN_INFO);
	assert(jTokenInfoClass != 0);
	/* create new CK_SLOT_INFO object */
	jTokenInfoObject = (*env)->AllocObject(env, jTokenInfoClass);
	assert(jTokenInfoObject != 0);


	/* set label */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "label", "[C");
	assert(jFieldID != 0);
	jTempCharArray = ckUTF8CharArrayToJCharArray(env, &(ckpTokenInfo->label[0]) ,32);
	(*env)->SetObjectField(env, jTokenInfoObject, jFieldID, jTempCharArray);

	/* set manufacturerID */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "manufacturerID", "[C");
	assert(jFieldID != 0);
	jTempCharArray = ckUTF8CharArrayToJCharArray(env, &(ckpTokenInfo->manufacturerID[0]) ,32);
	(*env)->SetObjectField(env, jTokenInfoObject, jFieldID, jTempCharArray);

	/* set model */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "model", "[C");
	assert(jFieldID != 0);
	jTempCharArray = ckUTF8CharArrayToJCharArray(env, &(ckpTokenInfo->model[0]) ,16);
	(*env)->SetObjectField(env, jTokenInfoObject, jFieldID, jTempCharArray);

	/* set serialNumber */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "serialNumber", "[C");
	assert(jFieldID != 0);
	jTempCharArray = ckCharArrayToJCharArray(env, &(ckpTokenInfo->serialNumber[0]) ,16);
	(*env)->SetObjectField(env, jTokenInfoObject, jFieldID, jTempCharArray);

	/* set flags */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "flags", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->flags));

	/* set ulMaxSessionCount */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "ulMaxSessionCount", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->ulMaxSessionCount));

	/* set ulSessionCount */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "ulSessionCount", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->ulSessionCount));

	/* set ulMaxRwSessionCount */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "ulMaxRwSessionCount", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->ulMaxRwSessionCount));

	/* set ulRwSessionCount */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "ulRwSessionCount", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->ulRwSessionCount));

	/* set ulMaxPinLen */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "ulMaxPinLen", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->ulMaxPinLen));

	/* set ulMinPinLen */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "ulMinPinLen", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->ulMinPinLen));

	/* set ulTotalPublicMemory */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "ulTotalPublicMemory", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->ulTotalPublicMemory));

	/* set ulFreePublicMemory */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "ulFreePublicMemory", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->ulFreePublicMemory));

	/* set ulTotalPrivateMemory */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "ulTotalPrivateMemory", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->ulTotalPrivateMemory));

	/* set ulFreePrivateMemory */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "ulFreePrivateMemory", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jTokenInfoObject, jFieldID, ckULongToJLong(ckpTokenInfo->ulFreePrivateMemory));


	/* set hardwareVersion */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "hardwareVersion", CLASS_NAME(CLASS_VERSION));
	assert(jFieldID != 0);
	jTempVersion = ckVersionPtrToJVersion(env, &(ckpTokenInfo->hardwareVersion));
	(*env)->SetObjectField(env, jTokenInfoObject, jFieldID, jTempVersion);

	/* set firmwareVersion */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "firmwareVersion", CLASS_NAME(CLASS_VERSION));
	assert(jFieldID != 0);
	jTempVersion = ckVersionPtrToJVersion(env, &(ckpTokenInfo->firmwareVersion));
	(*env)->SetObjectField(env, jTokenInfoObject, jFieldID, jTempVersion);

	/* set utcTime */
	jFieldID = (*env)->GetFieldID(env, jTokenInfoClass, "utcTime", "[C");
	assert(jFieldID != 0);
	jTempCharArray = ckCharArrayToJCharArray(env, &(ckpTokenInfo->utcTime[0]) ,16);
	(*env)->SetObjectField(env, jTokenInfoObject, jFieldID, jTempCharArray);

	return jTokenInfoObject ;
}

/*
 * converts a pointer to a CK_SESSION_INFO structure into a Java CK_SESSION_INFO Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpSessionInfo - the pointer to the CK_SESSION_INFO structure
 * @return - the new Java CK_SESSION_INFO object
 */
jobject ckSessionInfoPtrToJSessionInfo(JNIEnv *env, const CK_SESSION_INFO_PTR ckpSessionInfo)
{
	jclass jSessionInfoClass;
	jobject jSessionInfoObject;
	jfieldID jFieldID;

	/* load CK_SESSION_INFO class */
	jSessionInfoClass = (*env)->FindClass(env, CLASS_SESSION_INFO);
	assert(jSessionInfoClass != 0);
	/* create new CK_SESSION_INFO object */
	jSessionInfoObject = (*env)->AllocObject(env, jSessionInfoClass);
	assert(jSessionInfoObject != 0);

	/* set slotID */
	jFieldID = (*env)->GetFieldID(env, jSessionInfoClass, "slotID", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jSessionInfoObject, jFieldID, ckULongToJLong(ckpSessionInfo->slotID));

	/* set state */
	jFieldID = (*env)->GetFieldID(env, jSessionInfoClass, "state", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jSessionInfoObject, jFieldID, ckULongToJLong(ckpSessionInfo->state));

	/* set flags */
	jFieldID = (*env)->GetFieldID(env, jSessionInfoClass, "flags", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jSessionInfoObject, jFieldID, ckULongToJLong(ckpSessionInfo->flags));

	/* set ulDeviceError */
	jFieldID = (*env)->GetFieldID(env, jSessionInfoClass, "ulDeviceError", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jSessionInfoObject, jFieldID, ckULongToJLong(ckpSessionInfo->ulDeviceError));

	return jSessionInfoObject ;
}

/*
 * converts a pointer to a CK_MECHANISM_INFO structure into a Java CK_MECHANISM_INFO Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpMechanismInfo - the pointer to the CK_MECHANISM_INFO structure
 * @return - the new Java CK_MECHANISM_INFO object
 */
jobject ckMechanismInfoPtrToJMechanismInfo(JNIEnv *env, const CK_MECHANISM_INFO_PTR ckpMechanismInfo)
{
	jclass jMechanismInfoClass;
	jobject jMechanismInfoObject;
	jfieldID jFieldID;

	/* load CK_MECHANISM_INFO class */
	jMechanismInfoClass = (*env)->FindClass(env, CLASS_MECHANISM_INFO);
	assert(jMechanismInfoClass != 0);
	/* create new CK_MECHANISM_INFO object */
	jMechanismInfoObject = (*env)->AllocObject(env, jMechanismInfoClass);
	assert(jMechanismInfoObject != 0);


	/* set ulMinKeySize */
	jFieldID = (*env)->GetFieldID(env, jMechanismInfoClass, "ulMinKeySize", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jMechanismInfoObject, jFieldID, ckULongToJLong(ckpMechanismInfo->ulMinKeySize));

	/* set ulMaxKeySize */
	jFieldID = (*env)->GetFieldID(env, jMechanismInfoClass, "ulMaxKeySize", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jMechanismInfoObject, jFieldID, ckULongToJLong(ckpMechanismInfo->ulMaxKeySize));

	/* set flags */
	jFieldID = (*env)->GetFieldID(env, jMechanismInfoClass, "flags", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jMechanismInfoObject, jFieldID, ckULongToJLong(ckpMechanismInfo->flags));

	return jMechanismInfoObject ;
}

/*
 * converts a pointer to a CK_ATTRIBUTE structure into a Java CK_ATTRIBUTE Object.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpAttribute - the pointer to the CK_ATTRIBUTE structure
 * @return - the new Java CK_ATTRIBUTE object
 */
jobject ckAttributePtrToJAttribute(JNIEnv *env, const CK_ATTRIBUTE_PTR ckpAttribute, jobject obj, jlong jSessionHandle, jlong jObjectHandle, jboolean UseUtf8)
{
	jclass jAttributeClass;
	jobject jAttribute;
	jfieldID jFieldID;
	jobject jPValue = NULL_PTR;

	jAttributeClass = (*env)->FindClass(env, CLASS_ATTRIBUTE);
	assert(jAttributeClass != 0);
	jAttribute = (*env)->AllocObject(env, jAttributeClass);
	assert(jAttribute != 0);

	/* set type */
	jFieldID = (*env)->GetFieldID(env, jAttributeClass, "type", "J");
	assert(jFieldID != 0);
	(*env)->SetLongField(env, jAttribute, jFieldID, ckULongToJLong(ckpAttribute->type));

	/* set pValue */
	jFieldID = (*env)->GetFieldID(env, jAttributeClass, "pValue", "Ljava/lang/Object;");
	assert(jFieldID != 0);

	jPValue = ckAttributeValueToJObject(env, ckpAttribute, obj, jSessionHandle, jObjectHandle, UseUtf8);
	(*env)->SetObjectField(env, jAttribute, jFieldID, jPValue);

	return jAttribute ;
}

/*
 * converts a Java boolean object into a pointer to a CK_BBOOL value. The memory has to be
 * freed after use!
 *
 * @param env - used to call JNI functions to get the value out of the Java object
 * @param jObject - the "java/lang/Boolean" object to convert
 * @return - the pointer to the new CK_BBOOL value
 */
CK_BBOOL* jBooleanObjectToCKBBoolPtr(JNIEnv *env, jobject jObject)
{
	jclass jObjectClass;
	jmethodID jValueMethod;
	jboolean jValue;
	CK_BBOOL *ckpValue;

	jObjectClass = (*env)->FindClass(env, "java/lang/Boolean");
	assert(jObjectClass != 0);
	jValueMethod = (*env)->GetMethodID(env, jObjectClass, "booleanValue", "()Z");
	assert(jValueMethod != 0);
	jValue = (*env)->CallBooleanMethod(env, jObject, jValueMethod);
	ckpValue = (CK_BBOOL *) malloc(sizeof(CK_BBOOL));
  if (ckpValue == NULL_PTR) { throwOutOfMemoryError(env); return NULL_PTR; }
	*ckpValue = jBooleanToCKBBool(jValue);

	return ckpValue ;
}

/*
 * converts a Java byte object into a pointer to a CK_BYTE value. The memory has to be
 * freed after use!
 *
 * @param env - used to call JNI functions to get the value out of the Java object
 * @param jObject - the "java/lang/Byte" object to convert
 * @return - the pointer to the new CK_BYTE value
 */
CK_BYTE_PTR jByteObjectToCKBytePtr(JNIEnv *env, jobject jObject)
{
	jclass jObjectClass;
	jmethodID jValueMethod;
	jbyte jValue;
	CK_BYTE_PTR ckpValue;

	jObjectClass = (*env)->FindClass(env, "java/lang/Byte");
	assert(jObjectClass != 0);
	jValueMethod = (*env)->GetMethodID(env, jObjectClass, "byteValue", "()B");
	assert(jValueMethod != 0);
	jValue = (*env)->CallByteMethod(env, jObject, jValueMethod);
	ckpValue = (CK_BYTE_PTR) malloc(sizeof(CK_BYTE));
  if (ckpValue == NULL_PTR) { throwOutOfMemoryError(env); return NULL_PTR; }
	*ckpValue = jByteToCKByte(jValue);

	return ckpValue ;
}

/*
 * converts a Java integer object into a pointer to a CK_ULONG value. The memory has to be
 * freed after use!
 *
 * @param env - used to call JNI functions to get the value out of the Java object
 * @param jObject - the "java/lang/Integer" object to convert
 * @return - the pointer to the new CK_ULONG value
 */
CK_ULONG* jIntegerObjectToCKULongPtr(JNIEnv *env, jobject jObject)
{
	jclass jObjectClass;
	jmethodID jValueMethod;
	jint jValue;
	CK_ULONG *ckpValue;

	jObjectClass = (*env)->FindClass(env, "java/lang/Integer");
	assert(jObjectClass != 0);
	jValueMethod = (*env)->GetMethodID(env, jObjectClass, "intValue", "()I");
	assert(jValueMethod != 0);
	jValue = (*env)->CallIntMethod(env, jObject, jValueMethod);
	ckpValue = (CK_ULONG *) malloc(sizeof(CK_ULONG));
  if (ckpValue == NULL_PTR) { throwOutOfMemoryError(env); return NULL_PTR; }
	*ckpValue = jLongToCKLong(jValue);

	return ckpValue ;
}

/*
 * converts a Java long object into a pointer to a CK_ULONG value. The memory has to be
 * freed after use!
 *
 * @param env - used to call JNI functions to get the value out of the Java object
 * @param jObject - the "java/lang/Long" object to convert
 * @return - the pointer to the new CK_ULONG value
 */
CK_ULONG* jLongObjectToCKULongPtr(JNIEnv *env, jobject jObject)
{
	jclass jObjectClass;
	jmethodID jValueMethod;
	jlong jValue;
	CK_ULONG *ckpValue;

	jObjectClass = (*env)->FindClass(env, "java/lang/Long");
	assert(jObjectClass != 0);
	jValueMethod = (*env)->GetMethodID(env, jObjectClass, "longValue", "()J");
	assert(jValueMethod != 0);
	jValue = (*env)->CallLongMethod(env, jObject, jValueMethod);
	ckpValue = (CK_ULONG *) malloc(sizeof(CK_ULONG));
  if (ckpValue == NULL_PTR) { throwOutOfMemoryError(env); return NULL_PTR; }
	*ckpValue = jLongToCKULong(jValue);

	return ckpValue ;
}

/*
 * converts a Java char object into a pointer to a CK_CHAR value. The memory has to be
 * freed after use!
 *
 * @param env - used to call JNI functions to get the value out of the Java object
 * @param jObject - the "java/lang/Char" object to convert
 * @return - the pointer to the new CK_CHAR value
 */
CK_CHAR_PTR jCharObjectToCKCharPtr(JNIEnv *env, jobject jObject)
{
	jclass jObjectClass;
	jmethodID jValueMethod;
	jchar jValue;
	CK_CHAR_PTR ckpValue;

	jObjectClass = (*env)->FindClass(env, "java/lang/Char");
	assert(jObjectClass != 0);
	jValueMethod = (*env)->GetMethodID(env, jObjectClass, "charValue", "()C");
	assert(jValueMethod != 0);
	jValue = (*env)->CallCharMethod(env, jObject, jValueMethod);
	ckpValue = (CK_CHAR_PTR) malloc(sizeof(CK_CHAR));
  if (ckpValue == NULL_PTR) { throwOutOfMemoryError(env); return NULL_PTR; }
	*ckpValue = jCharToCKChar(jValue);

	return ckpValue ;
}

/*
 * converts a Java CK_VERSION object into a pointer to a CK_VERSION structure
 *
 * @param env - used to call JNI functions to get the values out of the Java object
 * @param jVersion - the Java CK_VERSION object to convert
 * @return - the pointer to the new CK_VERSION structure
 */
CK_VERSION_PTR jVersionToCKVersionPtr(JNIEnv *env, jobject jVersion)
{
	CK_VERSION_PTR ckpVersion;
	jclass jVersionClass;
	jfieldID jFieldID;
	jbyte jMajor, jMinor;

	/* allocate memory for CK_VERSION pointer */
	ckpVersion = (CK_VERSION_PTR) malloc(sizeof(CK_VERSION));
  if (ckpVersion == NULL_PTR) { throwOutOfMemoryError(env); return NULL_PTR; }

	/* get CK_VERSION class */
	jVersionClass = (*env)->GetObjectClass(env, jVersion);
	assert(jVersionClass != 0);

	/* get Major */
	jFieldID = (*env)->GetFieldID(env, jVersionClass, "major", "B");
	assert(jFieldID != 0);
	jMajor = (*env)->GetByteField(env, jVersion, jFieldID);
	ckpVersion->major = jByteToCKByte(jMajor);

	/* get Minor */
	jFieldID = (*env)->GetFieldID(env, jVersionClass, "minor", "B");
	assert(jFieldID != 0);
	jMinor = (*env)->GetByteField(env, jVersion, jFieldID);
	ckpVersion->minor = jByteToCKByte(jMinor);

	return ckpVersion ;
}


/*
 * converts a Java CK_DATE object into a pointer to a CK_DATE structure
 *
 * @param env - used to call JNI functions to get the values out of the Java object
 * @param jVersion - the Java CK_DATE object to convert
 * @return - the pointer to the new CK_DATE structure
 */
CK_DATE * jDateObjectPtrToCKDatePtr(JNIEnv *env, jobject jDate)
{
	CK_DATE * ckpDate;
  CK_ULONG ckLength;
	jclass jDateClass;
	jfieldID jFieldID;
	jobject jYear, jMonth, jDay;
  jchar *jTempChars;
  CK_ULONG i;

	/* allocate memory for CK_DATE pointer */
	ckpDate = (CK_DATE *) malloc(sizeof(CK_DATE));
  if (ckpDate == NULL_PTR) { throwOutOfMemoryError(env); return NULL_PTR; }

	/* get CK_DATE class */
	jDateClass = (*env)->FindClass(env, CLASS_DATE);
	assert(jDateClass != 0);

	/* get Year */
	jFieldID = (*env)->GetFieldID(env, jDateClass, "year", "[C");
	assert(jFieldID != 0);
	jYear = (*env)->GetObjectField(env, jDate, jFieldID);

  if (jYear == NULL_PTR) {
    ckpDate->year[0] = 0;
    ckpDate->year[1] = 0;
    ckpDate->year[2] = 0;
    ckpDate->year[3] = 0;
  } else {
  	ckLength = (*env)->GetArrayLength(env, jYear);
	  jTempChars = (jchar*) malloc((ckLength) * sizeof(jchar));
    if (jTempChars == NULL_PTR && ckLength!=0) { free(ckpDate); throwOutOfMemoryError(env); return NULL_PTR; }
  	(*env)->GetCharArrayRegion(env, jYear, 0, ckLength, jTempChars);
    for (i = 0; (i < ckLength) && (i < 4) ; i++) {
      ckpDate->year[i] = jCharToCKChar(jTempChars[i]);
    }
	  free(jTempChars);
  }

	/* get Month */
	jFieldID = (*env)->GetFieldID(env, jDateClass, "month", "[C");
	assert(jFieldID != 0);
	jMonth = (*env)->GetObjectField(env, jDate, jFieldID);

  if (jMonth == NULL_PTR) {
    ckpDate->month[0] = 0;
    ckpDate->month[1] = 0;
  } else {
  	ckLength = (*env)->GetArrayLength(env, jMonth);
	  jTempChars = (jchar*) malloc((ckLength) * sizeof(jchar));
    if (jTempChars == NULL_PTR && ckLength!=0) { free(ckpDate); throwOutOfMemoryError(env); return NULL_PTR; }
  	(*env)->GetCharArrayRegion(env, jMonth, 0, ckLength, jTempChars);
    for (i = 0; (i < ckLength) && (i < 4) ; i++) {
      ckpDate->month[i] = jCharToCKChar(jTempChars[i]);
    }
	  free(jTempChars);
  }

	/* get Day */
	jFieldID = (*env)->GetFieldID(env, jDateClass, "day", "[C");
	assert(jFieldID != 0);
	jDay = (*env)->GetObjectField(env, jDate, jFieldID);

  if (jDay == NULL_PTR) {
    ckpDate->day[0] = 0;
    ckpDate->day[1] = 0;
  } else {
  	ckLength = (*env)->GetArrayLength(env, jDay);
	  jTempChars = (jchar*) malloc((ckLength) * sizeof(jchar));
    if (jTempChars == NULL_PTR && ckLength!=0) { free(ckpDate); throwOutOfMemoryError(env); return NULL_PTR; }
  	(*env)->GetCharArrayRegion(env, jDay, 0, ckLength, jTempChars);
    for (i = 0; (i < ckLength) && (i < 4) ; i++) {
      ckpDate->day[i] = jCharToCKChar(jTempChars[i]);
    }
	  free(jTempChars);
  }

	return ckpDate ;
}


/*
 * converts a Java CK_ATTRIBUTE object into a CK_ATTRIBUTE structure
 *
 * @param env - used to call JNI functions to get the values out of the Java object
 * @param jAttribute - the Java CK_ATTRIBUTE object to convert
 * @return - the new CK_ATTRIBUTE structure
 */
CK_ATTRIBUTE jAttributeToCKAttribute(JNIEnv *env, jobject jAttribute, jboolean jUseUtf8)
{
	CK_ATTRIBUTE ckAttribute;
	jclass jAttributeClass;
	jfieldID jFieldID;
	jlong jType;
	jobject jPValue;

  TRACE0(tag_call, __FUNCTION__,"entering");

  /* get CK_ATTRIBUTE class */
	TRACE0(tag_debug, __FUNCTION__,"- getting attribute object class");
	jAttributeClass = (*env)->GetObjectClass(env, jAttribute);
	assert(jAttributeClass != 0);

	/* get type */
	TRACE0(tag_debug, __FUNCTION__,"- getting type field");
	jFieldID = (*env)->GetFieldID(env, jAttributeClass, "type", "J");
	assert(jFieldID != 0);
	jType = (*env)->GetLongField(env, jAttribute, jFieldID);
	TRACE1(tag_debug, __FUNCTION__,"  type=0x%X", (int)jType);

	/* get pValue */
	TRACE0(tag_debug, __FUNCTION__,"- getting pValue field");
	jFieldID = (*env)->GetFieldID(env, jAttributeClass, "pValue", "Ljava/lang/Object;");
	assert(jFieldID != 0);
	jPValue = (*env)->GetObjectField(env, jAttribute, jFieldID);
	TRACE1(tag_debug, __FUNCTION__,"  pValue=%p", jPValue);

	ckAttribute.type = jLongToCKULong(jType);
	TRACE0(tag_debug, __FUNCTION__,"- converting pValue to primitive object");

	if ((ckAttribute.type == 0x40000211) || (ckAttribute.type == 0x40000212)){
		TRACE0(tag_debug, __FUNCTION__,"  CKF_ARRAY_ATTRIBUTE:");
		if (jAttributeArrayToCKAttributeArray(env, jPValue, (CK_ATTRIBUTE_PTR*)&(ckAttribute.pValue), &(ckAttribute.ulValueLen), jUseUtf8)) {
			throwOutOfMemoryError(env);
		}
		ckAttribute.ulValueLen *= sizeof(CK_ATTRIBUTE);
	} else {
		/* convert the Java pValue object to a CK-type pValue pointer */
		jObjectToPrimitiveCKObjectPtrPtr(env, jPValue, &(ckAttribute.pValue), &(ckAttribute.ulValueLen), jUseUtf8);
	}

  TRACE0(tag_call, __FUNCTION__,"exiting ");

	return ckAttribute ;
}

/*
 * converts a Java CK_MECHANISM object into a CK_MECHANISM structure
 *
 * @param env - used to call JNI functions to get the values out of the Java object
 * @param jMechanism - the Java CK_MECHANISM object to convert
 * @return - the new CK_MECHANISM structure
 */
CK_MECHANISM jMechanismToCKMechanism(JNIEnv *env, jobject jMechanism, jboolean jUseUtf8)
{
	CK_MECHANISM ckMechanism;
	jclass jMechanismClass;
	jfieldID fieldID;
	jlong jMechanismType;
	jobject jParameter;

	/* get CK_MECHANISM class */
	jMechanismClass = (*env)->GetObjectClass(env, jMechanism);
	assert(jMechanismClass != 0);

	/* get mechanism */
	fieldID = (*env)->GetFieldID(env, jMechanismClass, "mechanism", "J");
	assert(fieldID != 0);
	jMechanismType = (*env)->GetLongField(env, jMechanism, fieldID);

	/* get pParameter */
	fieldID = (*env)->GetFieldID(env, jMechanismClass, "pParameter", "Ljava/lang/Object;");
	assert(fieldID != 0);
	jParameter = (*env)->GetObjectField(env, jMechanism, fieldID);

	ckMechanism.mechanism = jLongToCKULong(jMechanismType);

	/* convert the specific Java mechanism parameter object to a pointer to a CK-type mechanism
	 * structure
   */
	jMechanismParameterToCKMechanismParameter(env, jParameter, &(ckMechanism.pParameter), &(ckMechanism.ulParameterLen), jUseUtf8);

	return ckMechanism ;
}

/*
 * the following functions convert Attribute and Mechanism value pointers
 *
 * jobject ckAttributeValueToJObject(JNIEnv *env,
 *                                   const CK_ATTRIBUTE_PTR ckpAttribute);
 *
 * void jObjectToPrimitiveCKObjectPtrPtr(JNIEnv *env,
 *                                       jobject jObject,
 *                                       CK_VOID_PTR *ckpObjectPtr,
 *                                       CK_ULONG *pLength);
 *
 * void jMechanismParameterToCKMechanismParameter(JNIEnv *env,
 *                                                jobject jParam,
 *                                                CK_VOID_PTR *ckpParamPtr,
 *                                                CK_ULONG *ckpLength);
 *
 * These functions are used if a PKCS#11 mechanism or attribute structure gets
 * converted to a Java attribute or mechanism object or vice versa.
 *
 * ckAttributeValueToJObject converts a PKCS#11 attribute value pointer to a Java
 * object depending on the type of the Attribute. A PKCS#11 attribute value can
 * be a CK_ULONG, CK_BYTE[], CK_CHAR[], big integer, CK_BBOOL, CK_UTF8CHAR[],
 * CK_DATE or CK_FLAGS that gets converted to a corresponding Java object.
 *
 * jObjectToPrimitiveCKObjectPtrPtr is used by jAttributeToCKAttributePtr for
 * converting the Java attribute value to a PKCS#11 attribute value pointer.
 * For now only primitive datatypes and arrays of primitive datatypes can get
 * converted. Otherwise this function throws a PKCS#11Exception with the
 * errorcode CKR_VENDOR_DEFINED.
 *
 * jMechanismParameterToCKMechanismParameter converts a Java mechanism parameter
 * to a PKCS#11 mechanism parameter. First this function determines what mechanism
 * parameter the Java object is, then it allocates the memory for the new PKCS#11
 * structure and calls the corresponding function to convert the Java object to
 * a PKCS#11 mechanism parameter structure.
 */

/*
 * converts the pValue of a CK_ATTRIBUTE structure into a Java Object by checking the type
 * of the attribute.
 *
 * @param env - used to call JNI functions to create the new Java object
 * @param ckpAttribute - the pointer to the CK_ATTRIBUTE structure that contains the type
 *                       and the pValue to convert
 * @return - the new Java object of the CK-type pValue
 */
jobject ckAttributeValueToJObject(JNIEnv *env, const CK_ATTRIBUTE_PTR ckpAttribute, jobject obj, jlong jSessionHandle, jlong jObjectHandle, jboolean jUseUtf8)
{
	jint jValueLength;
	jobject jValueObject = NULL_PTR;
	CK_BBOOL useUtf8String;

	jValueLength = ckULongToJInt(ckpAttribute->ulValueLen);

	if ((jValueLength <= 0) || (ckpAttribute->pValue == NULL_PTR)) {
		return NULL_PTR ;
	}

	switch(ckpAttribute->type) {
		case CKA_CLASS:
			/* value CK_OBJECT_CLASS, defacto a CK_ULONG */
		case CKA_KEY_TYPE:
			/* value CK_KEY_TYPE, defacto a CK_ULONG */
		case CKA_CERTIFICATE_TYPE:
			/* value CK_CERTIFICATE_TYPE, defacto a CK_ULONG */
		case CKA_HW_FEATURE_TYPE:
			/* value CK_HW_FEATURE_TYPE, defacto a CK_ULONG */
		case CKA_MODULUS_BITS:
		case CKA_VALUE_BITS:
		case CKA_VALUE_LEN:
		case CKA_KEY_GEN_MECHANISM:
		case CKA_PRIME_BITS:
		case CKA_SUB_PRIME_BITS:
		case CKA_CERTIFICATE_CATEGORY:
		case CKA_JAVA_MIDP_SECURITY_DOMAIN:
		case CKA_MECHANISM_TYPE:
	  case CKA_PIXEL_X:
	  case CKA_PIXEL_Y:
	  case CKA_RESOLUTION:
	  case CKA_CHAR_ROWS:
	  case CKA_CHAR_COLUMNS:
	  case CKA_BITS_PER_PIXEL:
			/* value CK_ULONG */
			jValueObject = ckULongPtrToJLongObject(env, (CK_ULONG*) ckpAttribute->pValue);
			break;

			/* can be CK_BYTE[],CK_CHAR[] or big integer; defacto always CK_BYTE[] */
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
	  case CKA_REQUIRED_CMS_ATTRIBUTES:
	  case CKA_DEFAULT_CMS_ATTRIBUTES:
	  case CKA_SUPPORTED_CMS_ATTRIBUTES:
			/* value CK_BYTE[] */
			jValueObject = ckByteArrayToJByteArray(env, (CK_BYTE*) ckpAttribute->pValue, jValueLength);
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
			/* value CK_BBOOL */
			jValueObject = ckBBoolPtrToJBooleanObject(env, (CK_BBOOL*) ckpAttribute->pValue);
			break;

		case CKA_LABEL:
		case CKA_APPLICATION:
		case CKA_URL:
	  case CKA_CHAR_SETS:
	  case CKA_ENCODING_METHODS:
	  case CKA_MIME_TYPES:
			/* value RFC 2279 (UTF-8) string */
			useUtf8String = jBooleanToCKBBool(jUseUtf8);
			if(useUtf8String == TRUE){
				jValueObject = ckUTF8CharArrayToJCharArray(env, (CK_UTF8CHAR*) ckpAttribute->pValue, jValueLength);
			}else{
				jValueObject = ckCharArrayToJCharArray(env, (CK_UTF8CHAR*) ckpAttribute->pValue, jValueLength);
			}
			break;

		case CKA_START_DATE:
		case CKA_END_DATE:
			/* value CK_DATE */
			jValueObject = ckDatePtrToJDateObject(env, (CK_DATE*) ckpAttribute->pValue);
			break;

		case CKA_MODULUS:
		case CKA_PUBLIC_EXPONENT:
		case CKA_PRIME:
		case CKA_SUBPRIME:
		case CKA_BASE:
			/* value big integer, i.e. CK_BYTE[] */
			jValueObject = ckByteArrayToJByteArray(env, (CK_BYTE*) ckpAttribute->pValue, jValueLength);
			break;

		case CKA_AUTH_PIN_FLAGS:
			jValueObject = ckULongPtrToJLongObject(env, (CK_ULONG*) ckpAttribute->pValue);
			/* value FLAGS, defacto a CK_ULONG */
			break;

		case CKA_ALLOWED_MECHANISMS:
			jValueLength = jValueLength / sizeof(CK_MECHANISM_TYPE);
			jValueObject = ckULongArrayToJLongArray(env, (CK_ULONG*) ckpAttribute->pValue, jValueLength);
			break;

		case CKA_WRAP_TEMPLATE:
		case CKA_UNWRAP_TEMPLATE:
			jValueObject = ckAttributeArrayToJAttributeArray(env, (CK_ATTRIBUTE*) ckpAttribute->pValue, jValueLength, obj, jSessionHandle, jObjectHandle, jUseUtf8);
			break;

		case CKA_VENDOR_DEFINED:
			/* we make a CK_BYTE[] out of this */
			jValueObject = ckByteArrayToJByteArray(env, (CK_BYTE*) ckpAttribute->pValue, jValueLength);
			break;

		default:
			/* we make a CK_BYTE[] out of this */
			jValueObject = ckByteArrayToJByteArray(env, (CK_BYTE*) ckpAttribute->pValue, jValueLength);
			break;
	}

	return jValueObject ;
}

/*
 * converts a Java object into a pointer to CK-type or a CK-structure with the length in Bytes.
 * The memory of *ckpObjectPtr to be freed after use! This function is only used by
 * jAttributeToCKAttribute by now.
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jObject - the Java object to convert
 * @param ckpObjectPtr - the reference of the new pointer to the new CK-value or CK-structure
 * @param ckpLength - the reference of the length in bytes of the new CK-value or CK-structure
 */
void jObjectToPrimitiveCKObjectPtrPtr(JNIEnv *env, jobject jObject, CK_VOID_PTR *ckpObjectPtr, CK_ULONG *ckpLength, jboolean jUseUtf8)
{
	jclass jBooleanClass     = (*env)->FindClass(env, "java/lang/Boolean");
	jclass jByteClass        = (*env)->FindClass(env, "java/lang/Byte");
	jclass jCharacterClass   = (*env)->FindClass(env, "java/lang/Character");
	jclass jClassClass = (*env)->FindClass(env, "java/lang/Class");
	/* jclass jShortClass       = (*env)->FindClass(env, "java/lang/Short"); */
	jclass jIntegerClass     = (*env)->FindClass(env, "java/lang/Integer");
	jclass jLongClass        = (*env)->FindClass(env, "java/lang/Long");
	/* jclass jFloatClass       = (*env)->FindClass(env, "java/lang/Float"); */
	/* jclass jDoubleClass      = (*env)->FindClass(env, "java/lang/Double"); */
	jclass jDateClass      = (*env)->FindClass(env, CLASS_DATE);
	jclass jStringClass      = (*env)->FindClass(env, "java/lang/String");
	jclass jStringBufferClass      = (*env)->FindClass(env, "java/lang/StringBuffer");
	jclass jBooleanArrayClass = (*env)->FindClass(env, "[Z");
	jclass jByteArrayClass    = (*env)->FindClass(env, "[B");
	jclass jCharArrayClass    = (*env)->FindClass(env, "[C");
	/* jclass jShortArrayClass   = (*env)->FindClass(env, "[S"); */
	jclass jIntArrayClass     = (*env)->FindClass(env, "[I");
	jclass jLongArrayClass    = (*env)->FindClass(env, "[J");
	/* jclass jFloatArrayClass   = (*env)->FindClass(env, "[F"); */
	/* jclass jDoubleArrayClass  = (*env)->FindClass(env, "[D"); */
	jclass jObjectClass = (*env)->FindClass(env, "java/lang/Object");
  /*  jclass jObjectArrayClass = (*env)->FindClass(env, "[java/lang/Object"); */
  /* ATTENTION: jObjectArrayClass is always NULL_PTR !! */
  /* CK_ULONG ckArrayLength; */
	/* CK_VOID_PTR *ckpElementObject; */
	/* CK_ULONG ckElementLength; */
	/* CK_ULONG i; */
	jmethodID jMethod;
  jobject jClassObject;
  jstring jClassNameString;
  jstring jExceptionMessagePrefix;
  jobject jExceptionMessageStringBuffer;
  jstring jExceptionMessage;
  CK_BBOOL ckUseUtf8;
/*#if DEBUG
  char buffer[buffer_size];
  int i = 0;
  for(i; i < buffer_size; i++)
  	buffer[i] = '\0';
#endif*/

  TRACE0(tag_call, __FUNCTION__,"entering");

	if (jObject == NULL_PTR) {
		*ckpObjectPtr = NULL_PTR;
		*ckpLength = 0;
		TRACE0(tag_debug, __FUNCTION__, "- converted NULL_PTR value");
	} else if ((*env)->IsInstanceOf(env, jObject, jLongClass)) {
		*ckpObjectPtr = jLongObjectToCKULongPtr(env, jObject);
		*ckpLength = sizeof(CK_ULONG);
		TRACE1(tag_debug, __FUNCTION__,"- converted long value %X", *((unsigned int *) *ckpObjectPtr));
	} else if ((*env)->IsInstanceOf(env, jObject, jBooleanClass)) {
		*ckpObjectPtr = jBooleanObjectToCKBBoolPtr(env, jObject);
		*ckpLength = sizeof(CK_BBOOL);
		TRACE0(tag_debug, __FUNCTION__,(*((CK_BBOOL *) *ckpObjectPtr) == TRUE) ? "- converted boolean value TRUE>" : "- converted boolean value FALSE>");
	} else if ((*env)->IsInstanceOf(env, jObject, jByteArrayClass)) {
		jByteArrayToCKByteArray(env, jObject, (CK_BYTE_PTR*)ckpObjectPtr, ckpLength);
/*#if DEBUG
		byteArrayToHexString((char *)(*ckpObjectPtr), *ckpLength, buffer, buffer_size);
		TRACE1(tag_debug, __FUNCTION__, "- converted byte array: %s", buffer);
#endif*/
	} else if ((*env)->IsInstanceOf(env, jObject, jCharArrayClass)) {
		ckUseUtf8 = jBooleanToCKBBool(jUseUtf8);
		if(ckUseUtf8 == TRUE){
			jCharArrayToCKUTF8CharArray(env, jObject, (CK_UTF8CHAR_PTR*)ckpObjectPtr, ckpLength);
		}else{
			jCharArrayToCKCharArray(env, jObject, (CK_UTF8CHAR_PTR*)ckpObjectPtr, ckpLength);
		}
		TRACE0(tag_debug, __FUNCTION__, "- converted char array");
	} else if ((*env)->IsInstanceOf(env, jObject, jByteClass)) {
		*ckpObjectPtr = jByteObjectToCKBytePtr(env, jObject);
		*ckpLength = sizeof(CK_BYTE);
		TRACE1(tag_debug, __FUNCTION__,"- converted byte value %X", *((CK_BYTE *) *ckpObjectPtr));
	} else if ((*env)->IsInstanceOf(env, jObject, jDateClass)) {
		*ckpObjectPtr = jDateObjectPtrToCKDatePtr(env, jObject);
		*ckpLength = sizeof(CK_DATE);
		TRACE3(tag_debug, __FUNCTION__,"- converted date value %.4s-%.2s-%.2s", (*((CK_DATE *) *ckpObjectPtr)).year,
                                                    (*((CK_DATE *) *ckpObjectPtr)).month,
                                                    (*((CK_DATE *) *ckpObjectPtr)).day);
	} else if ((*env)->IsInstanceOf(env, jObject, jCharacterClass)) {
		*ckpObjectPtr = jCharObjectToCKCharPtr(env, jObject);
		*ckpLength = sizeof(CK_UTF8CHAR);
		TRACE1(tag_debug, __FUNCTION__,"- converted char value %c", *((CK_CHAR *) *ckpObjectPtr));
	} else if ((*env)->IsInstanceOf(env, jObject, jIntegerClass)) {
		*ckpObjectPtr = jIntegerObjectToCKULongPtr(env, jObject);
		*ckpLength = sizeof(CK_ULONG);
		TRACE1(tag_debug, __FUNCTION__,"- converted integer value %X", *((unsigned int*) *ckpObjectPtr));
	} else if ((*env)->IsInstanceOf(env, jObject, jBooleanArrayClass)) {
		jBooleanArrayToCKBBoolArray(env, jObject, (CK_BBOOL**)ckpObjectPtr, ckpLength);
		TRACE0(tag_debug, __FUNCTION__, "- converted boolean array");
	} else if ((*env)->IsInstanceOf(env, jObject, jIntArrayClass)) {
		jLongArrayToCKULongArray(env, jObject, (CK_ULONG_PTR*)ckpObjectPtr, ckpLength);
		TRACE0(tag_debug, __FUNCTION__, "- converted int array");
} else if ((*env)->IsInstanceOf(env, jObject, jLongArrayClass)) {
		jLongArrayToCKULongArray(env, jObject, (CK_ULONG_PTR*)ckpObjectPtr, ckpLength);
		*ckpLength = *ckpLength * sizeof(CK_MECHANISM_TYPE);
		TRACE0(tag_debug, __FUNCTION__, "- converted long array");
} else if ((*env)->IsInstanceOf(env, jObject, jStringClass)) {
		jStringToCKUTF8CharArray(env, jObject, (CK_UTF8CHAR_PTR*)ckpObjectPtr, ckpLength);
		TRACE0(tag_debug, __FUNCTION__, "- converted string");

    /* a Java object array is not used by CK_ATTRIBUTE by now... */
/*	} else if ((*env)->IsInstanceOf(env, jObject, jObjectArrayClass)) {
		ckArrayLength = (*env)->GetArrayLength(env, (jarray) jObject);
		ckpObjectPtr = (CK_VOID_PTR_PTR) malloc(sizeof(CK_VOID_PTR) * ckArrayLength);
    if (ckpObjectPtr == NULL_PTR && ckArrayLength!=0) { *ckpObjectPtr = NULL_PTR; throwOutOfMemoryError(env); return NULL_PTR; }
		*ckpLength = 0;
		for (i = 0; i < ckArrayLength; i++) {
			jObjectToPrimitiveCKObjectPtrPtr(env, (*env)->GetObjectArrayElement(env, (jarray) jObject, i),
									   ckpElementObject, &ckElementLength);
			(*ckpObjectPtr)[i] = *ckpElementObject;
			*ckpLength += ckElementLength;
		}
*/
	} else {
		TRACE0(tag_error, __FUNCTION__, "- Java object of this class cannot be converted to native PKCS#11 type");

		/* type of jObject unknown, throw PKCS11RuntimeException */
	  jMethod = (*env)->GetMethodID(env, jObjectClass, "getClass", "()Ljava/lang/Class;");
	  assert(jMethod != 0);
    jClassObject = (*env)->CallObjectMethod(env, jObject, jMethod);
	  assert(jClassObject != 0);
	  jMethod = (*env)->GetMethodID(env, jClassClass, "getName", "()Ljava/lang/String;");
	  assert(jMethod != 0);
    jClassNameString = (jstring)
        (*env)->CallObjectMethod(env, jClassObject, jMethod);
	  assert(jClassNameString != 0);
    jExceptionMessagePrefix = (*env)->NewStringUTF(env, "Java object of this class cannot be converted to native PKCS#11 type: ");
	  jMethod = (*env)->GetMethodID(env, jStringBufferClass, "<init>", "(Ljava/lang/String;)V");
	  assert(jMethod != 0);
    jExceptionMessageStringBuffer = (*env)->NewObject(env, jStringBufferClass, jMethod, jExceptionMessagePrefix);
	  assert(jClassNameString != 0);
	  jMethod = (*env)->GetMethodID(env, jStringBufferClass, "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
	  assert(jMethod != 0);
    jExceptionMessage = (jstring)
         (*env)->CallObjectMethod(env, jExceptionMessageStringBuffer, jMethod, jClassNameString);
	  assert(jExceptionMessage != 0);

	  throwPKCS11RuntimeException(env, jExceptionMessage);

		*ckpObjectPtr = NULL_PTR;
		*ckpLength = 0;
	}

  TRACE0(tag_call, __FUNCTION__,"exiting ");
}

/*
 * the following functions convert a Java mechanism parameter object to a PKCS#11
 * mechanism parameter structure
 *
 * CK_<Param>_PARAMS j<Param>ParamToCK<Param>Param(JNIEnv *env,
 *                                                 jobject jParam);
 *
 * These functions get a Java object, that must be the right Java mechanism
 * object and they return the new PKCS#11 mechanism parameter structure.
 * Every field of the Java object is retrieved, gets converted to a corresponding
 * PKCS#11 type and is set in the new PKCS#11 structure.
 */

/*
 * converts the given Java mechanism parameter to a CK mechanism parameter structure
 * and store the length in bytes in the length variable.
 * The memory of *ckpParamPtr has to be freed after use!
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java mechanism parameter object to convert
 * @param ckpParamPtr - the reference of the new pointer to the new CK mechanism parameter
 *                      structure
 * @param ckpLength - the reference of the length in bytes of the new CK mechanism parameter
 *                    structure
 */
void jMechanismParameterToCKMechanismParameter(JNIEnv *env, jobject jParam, CK_VOID_PTR *ckpParamPtr, CK_ULONG *ckpLength, jboolean jUseUtf8)
{
	/* get all Java mechanism parameter classes */
	jclass jByteArrayClass    = (*env)->FindClass(env, "[B");
	jclass jLongClass        = (*env)->FindClass(env, "java/lang/Long");
	jclass jVersionClass    = (*env)->FindClass(env, CLASS_VERSION);
	jclass jRsaPkcsOaepParamsClass = (*env)->FindClass(env, CLASS_RSA_PKCS_OAEP_PARAMS);
	jclass jKeaDeriveParamsClass = (*env)->FindClass(env, CLASS_KEA_DERIVE_PARAMS);
  jclass jRc2CbcParamsClass = (*env)->FindClass(env, CLASS_RC2_CBC_PARAMS);
	jclass jRc2MacGeneralParamsClass = (*env)->FindClass(env, CLASS_RC2_MAC_GENERAL_PARAMS);
	jclass jRc5ParamsClass = (*env)->FindClass(env, CLASS_RC5_PARAMS);
  jclass jRc5CbcParamsClass = (*env)->FindClass(env, CLASS_RC5_CBC_PARAMS);
	jclass jRc5MacGeneralParamsClass = (*env)->FindClass(env, CLASS_RC5_MAC_GENERAL_PARAMS);
	jclass jSkipjackPrivateWrapParamsClass = (*env)->FindClass(env, CLASS_SKIPJACK_PRIVATE_WRAP_PARAMS);
	jclass jSkipjackRelayxParamsClass = (*env)->FindClass(env, CLASS_SKIPJACK_RELAYX_PARAMS);
	jclass jPbeParamsClass = (*env)->FindClass(env, CLASS_PBE_PARAMS);
	jclass jPkcs5Pbkd2ParamsClass = (*env)->FindClass(env, CLASS_PKCS5_PBKD2_PARAMS);
	jclass jKeyWrapSetOaepParamsClass = (*env)->FindClass(env, CLASS_KEY_WRAP_SET_OAEP_PARAMS);
  jclass jKeyDerivationStringDataClass = (*env)->FindClass(env, CLASS_KEY_DERIVATION_STRING_DATA);
	jclass jSsl3MasterKeyDeriveParamsClass = (*env)->FindClass(env, CLASS_SSL3_MASTER_KEY_DERIVE_PARAMS);
	jclass jSsl3KeyMatParamsClass = (*env)->FindClass(env, CLASS_SSL3_KEY_MAT_PARAMS);

	jclass jRsaPkcsPssParamsClass = (*env)->FindClass(env, CLASS_RSA_PKCS_PSS_PARAMS);
	jclass jEcdh1DeriveParamsClass = (*env)->FindClass(env, CLASS_ECDH1_DERIVE_PARAMS);
	jclass jEcdh2DeriveParamsClass = (*env)->FindClass(env, CLASS_ECDH2_DERIVE_PARAMS);
	jclass jX942Dh1DeriveParamsClass = (*env)->FindClass(env, CLASS_X9_42_DH1_DERIVE_PARAMS);
	jclass jX942Dh2DeriveParamsClass = (*env)->FindClass(env, CLASS_X9_42_DH2_DERIVE_PARAMS);
	jclass jDesCbcEncryptDataParamsClass = (*env)->FindClass(env, CLASS_DES_CBC_ENCRYPT_DATA_PARAMS);
	jclass jAesCbcEncryptDataParamsClass = (*env)->FindClass(env, CLASS_AES_CBC_ENCRYPT_DATA_PARAMS);
	jclass jGcmParamsClass = (*env)->FindClass(env, CLASS_GCM_PARAMS);
	jclass jCcmParamsClass = (*env)->FindClass(env, CLASS_CCM_PARAMS);

  /* first check the most common cases */
	if (jParam == NULL_PTR) {
		*ckpParamPtr = NULL_PTR;
		*ckpLength = 0;
  } else if ((*env)->IsInstanceOf(env, jParam, jByteArrayClass)) {
    jByteArrayToCKByteArray(env, jParam, (CK_BYTE_PTR *)ckpParamPtr, ckpLength);
  } else if ((*env)->IsInstanceOf(env, jParam, jLongClass)) {
		*ckpParamPtr = jLongObjectToCKULongPtr(env, jParam);
		*ckpLength = sizeof(CK_ULONG);
  } else if ((*env)->IsInstanceOf(env, jParam, jVersionClass)) {
		/*
		 * CK_VERSION used by CKM_SSL3_PRE_MASTER_KEY_GEN
		 */

		CK_VERSION_PTR ckpParam;

		/* convert jParameter to CKParameter */
		ckpParam = jVersionToCKVersionPtr(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_VERSION);
		*ckpParamPtr = ckpParam;

  } else if ((*env)->IsInstanceOf(env, jParam, jRsaPkcsOaepParamsClass)) {
		/*
		 * CK_RSA_PKCS_OAEP_PARAMS
		 */

		CK_RSA_PKCS_OAEP_PARAMS_PTR ckpParam;

		ckpParam = (CK_RSA_PKCS_OAEP_PARAMS_PTR) malloc(sizeof(CK_RSA_PKCS_OAEP_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jRsaPkcsOaepParamToCKRsaPkcsOaepParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_RSA_PKCS_OAEP_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jKeaDeriveParamsClass)) {
		/*
		 * CK_KEA_DERIVE_PARAMS
		 */

		CK_KEA_DERIVE_PARAMS_PTR ckpParam;

		ckpParam = (CK_KEA_DERIVE_PARAMS_PTR) malloc(sizeof(CK_KEA_DERIVE_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jKeaDeriveParamToCKKeaDeriveParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_KEA_DERIVE_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jRc2CbcParamsClass)) {
		/*
		 * CK_RC2_CBC_PARAMS
		 */

		CK_RC2_CBC_PARAMS_PTR ckpParam;

		ckpParam = (CK_RC2_CBC_PARAMS_PTR) malloc(sizeof(CK_RC2_CBC_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jRc2CbcParamToCKRc2CbcParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_RC2_CBC_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jRc2MacGeneralParamsClass)) {
		/*
		 * CK_RC2_MAC_GENERAL_PARAMS
		 */

		CK_RC2_MAC_GENERAL_PARAMS_PTR ckpParam;

		ckpParam = (CK_RC2_MAC_GENERAL_PARAMS_PTR) malloc(sizeof(CK_RC2_MAC_GENERAL_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jRc2MacGeneralParamToCKRc2MacGeneralParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_RC2_MAC_GENERAL_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jRc5ParamsClass)) {
		/*
		 * CK_RC5_PARAMS
		 */

		CK_RC5_PARAMS_PTR ckpParam;

		ckpParam = (CK_RC5_PARAMS_PTR) malloc(sizeof(CK_RC5_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jRc5ParamToCKRc5Param(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_RC5_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jRc5CbcParamsClass)) {
		/*
		 * CK_RC5_CBC_PARAMS
		 */

		CK_RC5_CBC_PARAMS_PTR ckpParam;

		ckpParam = (CK_RC5_CBC_PARAMS_PTR) malloc(sizeof(CK_RC5_CBC_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jRc5CbcParamToCKRc5CbcParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_RC5_CBC_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jRc5MacGeneralParamsClass)) {
		/*
		 * CK_RC5_MAC_GENERAL_PARAMS
		 */

		CK_RC5_MAC_GENERAL_PARAMS_PTR ckpParam;

		ckpParam = (CK_RC5_MAC_GENERAL_PARAMS_PTR) malloc(sizeof(CK_RC5_MAC_GENERAL_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jRc5MacGeneralParamToCKRc5MacGeneralParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_RC5_MAC_GENERAL_PARAMS);

		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jSkipjackPrivateWrapParamsClass)) {
		/*
		 * CK_SKIPJACK_PRIVATE_WRAP_PARAMS
		 */

		CK_SKIPJACK_PRIVATE_WRAP_PTR ckpParam;

		ckpParam = (CK_SKIPJACK_PRIVATE_WRAP_PTR) malloc(sizeof(CK_SKIPJACK_PRIVATE_WRAP_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jSkipjackPrivateWrapParamToCKSkipjackPrivateWrapParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_SKIPJACK_PRIVATE_WRAP_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jSkipjackRelayxParamsClass)) {
		/*
		 * CK_SKIPJACK_RELAYX_PARAMS
		 */

		CK_SKIPJACK_RELAYX_PARAMS_PTR ckpParam;

		ckpParam = (CK_SKIPJACK_RELAYX_PARAMS_PTR) malloc(sizeof(CK_SKIPJACK_RELAYX_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jSkipjackRelayxParamToCKSkipjackRelayxParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_SKIPJACK_RELAYX_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jPbeParamsClass)) {
		/*
		 * CK_PBE_PARAMS
		 */

		CK_PBE_PARAMS_PTR ckpParam;

		ckpParam = (CK_PBE_PARAMS_PTR) malloc(sizeof(CK_PBE_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jPbeParamToCKPbeParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_PBE_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jPkcs5Pbkd2ParamsClass)) {
		/*
		 * CK_PKCS5_PBKD2_PARAMS
		 */

		CK_PKCS5_PBKD2_PARAMS_PTR ckpParam;

		ckpParam = (CK_PKCS5_PBKD2_PARAMS_PTR) malloc(sizeof(CK_PKCS5_PBKD2_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jPkcs5Pbkd2ParamToCKPkcs5Pbkd2Param(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_PKCS5_PBKD2_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jKeyDerivationStringDataClass)) {
		/*
		 * CK_KEY_DERIVATION_STRING_DATA
		 */

		CK_KEY_DERIVATION_STRING_DATA_PTR ckpParam;

		ckpParam = (CK_KEY_DERIVATION_STRING_DATA_PTR) malloc(sizeof(CK_KEY_DERIVATION_STRING_DATA));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jKeyDerivationStringDataToCKKeyDerivationStringData(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_KEY_DERIVATION_STRING_DATA);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jKeyWrapSetOaepParamsClass)) {
		/*
		 * CK_KEY_WRAP_SET_OAEP_PARAMS
		 */

		CK_KEY_WRAP_SET_OAEP_PARAMS_PTR ckpParam;

		ckpParam = (CK_KEY_WRAP_SET_OAEP_PARAMS_PTR) malloc(sizeof(CK_KEY_WRAP_SET_OAEP_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jKeyWrapSetOaepParamToCKKeyWrapSetOaepParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_KEY_WRAP_SET_OAEP_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jSsl3MasterKeyDeriveParamsClass)) {
		/*
		 * CK_SSL3_MASTER_KEY_DERIVE_PARAMS
		 */

		CK_SSL3_MASTER_KEY_DERIVE_PARAMS_PTR ckpParam;

		ckpParam = (CK_SSL3_MASTER_KEY_DERIVE_PARAMS_PTR) malloc(sizeof(CK_SSL3_MASTER_KEY_DERIVE_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jSsl3MasterKeyDeriveParamToCKSsl3MasterKeyDeriveParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_SSL3_MASTER_KEY_DERIVE_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jSsl3KeyMatParamsClass)) {
		/*
		 * CK_SSL3_KEY_MAT_PARAMS
		 */

		CK_SSL3_KEY_MAT_PARAMS_PTR ckpParam;

		ckpParam = (CK_SSL3_KEY_MAT_PARAMS_PTR) malloc(sizeof(CK_SSL3_KEY_MAT_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jSsl3KeyMatParamToCKSsl3KeyMatParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_SSL3_KEY_MAT_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jRsaPkcsPssParamsClass)) {
		/*
		 * CK_RSA_PKCS_PSS_PARAMS
		 */

		CK_RSA_PKCS_PSS_PARAMS_PTR ckpParam;

		ckpParam = (CK_RSA_PKCS_PSS_PARAMS_PTR) malloc(sizeof(CK_RSA_PKCS_PSS_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jRsaPkcsPssParamToCKRsaPkcsPssParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_RSA_PKCS_PSS_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jEcdh1DeriveParamsClass)) {
		/*
		 * CK_ECDH1_DERIVE_PARAMS
		 */

		CK_ECDH1_DERIVE_PARAMS_PTR ckpParam;

		ckpParam = (CK_ECDH1_DERIVE_PARAMS_PTR) malloc(sizeof(CK_ECDH1_DERIVE_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jEcdh1DeriveParamToCKEcdh1DeriveParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_ECDH1_DERIVE_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jEcdh2DeriveParamsClass)) {
		/*
		 * CK_ECDH2_DERIVE_PARAMS
		 */

		CK_ECDH2_DERIVE_PARAMS_PTR ckpParam;

		ckpParam = (CK_ECDH2_DERIVE_PARAMS_PTR) malloc(sizeof(CK_ECDH2_DERIVE_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jEcdh2DeriveParamToCKEcdh2DeriveParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_ECDH2_DERIVE_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jX942Dh1DeriveParamsClass)) {
		/*
		 * CK_X9_42_DH1_DERIVE_PARAMS
		 */

		CK_X9_42_DH1_DERIVE_PARAMS_PTR ckpParam;

		ckpParam = (CK_X9_42_DH1_DERIVE_PARAMS_PTR) malloc(sizeof(CK_X9_42_DH1_DERIVE_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jX942Dh1DeriveParamToCKX942Dh1DeriveParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_X9_42_DH1_DERIVE_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jX942Dh2DeriveParamsClass)) {
		/*
		 * CK_X9_42_DH2_DERIVE_PARAMS
		 */

		CK_X9_42_DH2_DERIVE_PARAMS_PTR ckpParam;

		ckpParam = (CK_X9_42_DH2_DERIVE_PARAMS_PTR) malloc(sizeof(CK_X9_42_DH2_DERIVE_PARAMS));
    if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jX942Dh2DeriveParamToCKX942Dh2DeriveParam(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_X9_42_DH2_DERIVE_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jDesCbcEncryptDataParamsClass)) {
		/*
		* CK_DES_CBC_ENCRYPT_DATA_PARAMS
		*/

		CK_DES_CBC_ENCRYPT_DATA_PARAMS_PTR ckpParam;

		ckpParam = (CK_DES_CBC_ENCRYPT_DATA_PARAMS_PTR) malloc(sizeof(CK_DES_CBC_ENCRYPT_DATA_PARAMS));
		if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jDesCbcEncryptDataParamToCKDesCbcEncryptData(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_DES_CBC_ENCRYPT_DATA_PARAMS);
		*ckpParamPtr = ckpParam;

	} else if ((*env)->IsInstanceOf(env, jParam, jAesCbcEncryptDataParamsClass)) {
		/*
		* CK_AES_CBC_ENCRYPT_DATA_PARAMS
		*/

		CK_AES_CBC_ENCRYPT_DATA_PARAMS_PTR ckpParam;

		ckpParam = (CK_AES_CBC_ENCRYPT_DATA_PARAMS_PTR) malloc(sizeof(CK_AES_CBC_ENCRYPT_DATA_PARAMS));
		if (ckpParam == NULL_PTR) { *ckpParamPtr = NULL_PTR; throwOutOfMemoryError(env); return; }

		/* convert jParameter to CKParameter */
		*ckpParam = jAesCbcEncryptDataParamToCKAesCbcEncryptData(env, jParam);

		/* get length and pointer of parameter */
		*ckpLength = sizeof(CK_AES_CBC_ENCRYPT_DATA_PARAMS);
		*ckpParamPtr = ckpParam;

    } else if ((*env)->IsInstanceOf(env, jParam, jGcmParamsClass)) {
        /*
        * CK_GCM_ENCRYPT_DATA_PARAMS
        */
        CK_GCM_PARAMS_PTR ckpParam;
        ckpParam = (CK_GCM_PARAMS_PTR) malloc(sizeof(CK_GCM_PARAMS));
        if (ckpParam == NULL_PTR) {
            *ckpParamPtr = NULL_PTR;
            throwOutOfMemoryError(env);
            return;
        }

        /* convert jParameter to CKParameter */
        *ckpParam = jGcmParamToCKGcmData(env, jParam);

        /* get length and pointer of parameter */
        *ckpLength = sizeof(CK_GCM_PARAMS);
        *ckpParamPtr = ckpParam;

    } else if ((*env)->IsInstanceOf(env, jParam, jCcmParamsClass)) {
        /*
        * CK_CCM_ENCRYPT_DATA_PARAMS
    	*/

        CK_CCM_PARAMS_PTR ckpParam;
        ckpParam = (CK_CCM_PARAMS_PTR) malloc(sizeof(CK_CCM_PARAMS));
        if (ckpParam == NULL_PTR) {
            *ckpParamPtr = NULL_PTR;
            throwOutOfMemoryError(env);
            return;
        }

        /* convert jParameter to CKParameter */
        *ckpParam = jCcmParamToCKCcmData(env, jParam);

        /* get length and pointer of parameter */
        *ckpLength = sizeof(CK_CCM_PARAMS);
        *ckpParamPtr = ckpParam;
    } else {
    /* if everything faild up to here */
    /* try if the parameter is a primitive Java type */
    jObjectToPrimitiveCKObjectPtrPtr(env, jParam, ckpParamPtr, ckpLength, jUseUtf8);
		/* *ckpParamPtr = jObjectToCKVoidPtr(jParam); */
		/* *ckpLength = 1; */
	}
}


