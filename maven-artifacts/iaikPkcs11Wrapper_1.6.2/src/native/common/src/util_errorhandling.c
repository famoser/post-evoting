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
/* Memory and error handling                                                  */
/* ************************************************************************** */

/*
 * function to check the given return value and to convert an invalid PKCS#11
 * return value into a PKCS#11Exception
 *
 * This function generates a PKCS#11Exception with the returnValue as the errorcode
 * if the returnValue is not CKR_OK. The function returns 0, if the returnValue is
 * CKR_OK. Otherwise, it returns the returnValue as a jLong.
 *
 * @param env - used to call JNI functions and to get the Exception class
 * @param returnValue - of the PKCS#11 function
 * @param callerMethodName - name of the caller-function
 */
jlong ckAssertReturnValueOK(JNIEnv * env, CK_RV returnValue, const char *callerMethodName)
{
    if (returnValue == CKR_OK) {
	return 0L;
    } else {
	return throwException(env, returnValue, callerMethodName);
    }
}

/*
 * function to check the given return value and to convert an invalid PKCS#11
 * return value into a PKCS#11Exception
 *
 * This function generates a PKCS#11Exception with the returnValue as the errorcode
 * if the returnValue is not CKR_OK and not CKR_ATTRIBUTE_SENSITIVE or
 * CKR_ATTRIBUTE_TYPE_INVALID for ckAttributesLength larger than 1. The function
 * returns 0, if the returnValue is CKR_OK or if it is CKR_ATTRIBUTE_SENSITIVE or
 * CKR_ATTRIBUTE_TYPE_INVALID for ckAttributesLength larger than 1.
 * Otherwise, it returns the returnValue as a jLong.
 *
 * @param env - used to call JNI functions and to get the Exception class
 * @param returnValue - of the PKCS#11 function
 * @param callerMethodName - name of the caller-function
 * @param ckAttributesLength - length of the attribute array handled in the caller-function
 */
jlong ckAssertAttributeReturnValueOK(JNIEnv * env, CK_RV returnValue, const char *callerMethodName,
				     CK_ULONG ckAttributesLength)
{
    if (returnValue == CKR_OK || ((returnValue == CKR_ATTRIBUTE_SENSITIVE || returnValue == CKR_ATTRIBUTE_TYPE_INVALID)
				  && ckAttributesLength > 1)) {
	return 0L;
    } else {
	return throwException(env, returnValue, callerMethodName);
    }
}

/*
 * this function throws an OutOfMemoryError, e.g. in case a malloc did fail to
 * allocate memory.
 *
 * @param env Used to call JNI functions and to get the Exception class.
 */
void throwOutOfMemoryError(JNIEnv * env)
{
    jclass jOutOfMemoryErrorClass;
    jmethodID jConstructor;
    jthrowable jOutOfMemoryError;

    jOutOfMemoryErrorClass = (*env)->FindClass(env, CLASS_OUT_OF_MEMORY_ERROR);
    assert(jOutOfMemoryErrorClass != 0);

    jConstructor = (*env)->GetMethodID(env, jOutOfMemoryErrorClass, "<init>", "()V");
    assert(jConstructor != 0);
    jOutOfMemoryError = (jthrowable) (*env)->NewObject(env, jOutOfMemoryErrorClass, jConstructor);
    (*env)->Throw(env, jOutOfMemoryError);
}

/*
 * this function simply throws a FileNotFoundException
 *
 * @param env Used to call JNI functions and to get the Exception class.
 * @param jmessage The message string of the Exception object.
 */
void throwFileNotFoundException(JNIEnv * env, jstring jmessage)
{
    jclass jFileNotFoundExceptionClass;
    jmethodID jConstructor;
    jthrowable jFileNotFoundException;

    jFileNotFoundExceptionClass = (*env)->FindClass(env, CLASS_FILE_NOT_FOUND_EXCEPTION);
    assert(jFileNotFoundExceptionClass != 0);

    jConstructor = (*env)->GetMethodID(env, jFileNotFoundExceptionClass, "<init>", "(Ljava/lang/String;)V");
    assert(jConstructor != 0);
    jFileNotFoundException = (jthrowable) (*env)->NewObject(env, jFileNotFoundExceptionClass, jConstructor, jmessage);
    (*env)->Throw(env, jFileNotFoundException);
}

/*
 * this function simply throws an IOException
 *
 * @param env Used to call JNI functions and to get the Exception class.
 * @param message The message string of the Exception object.
 */
void throwIOException(JNIEnv * env, const char *message)
{
    jclass jIOExceptionClass;

    jIOExceptionClass = (*env)->FindClass(env, CLASS_IO_EXCEPTION);
    assert(jIOExceptionClass != 0);

    (*env)->ThrowNew(env, jIOExceptionClass, message);
}

/*
 * this function simply throws an IOException and takes a unicode
 * messge.
 *
 * @param env Used to call JNI functions and to get the Exception class.
 * @param message The unicode message string of the Exception object.
 */
void throwIOExceptionUnicodeMessage(JNIEnv * env, const unsigned short *message)
{
    jclass jIOExceptionClass;
    jmethodID jConstructor;
    jthrowable jIOException;
    jstring jmessage;
    jsize length;
    short *currentCharacter;

    jIOExceptionClass = (*env)->FindClass(env, CLASS_IO_EXCEPTION);
    assert(jIOExceptionClass != 0);

    length = 0;
    if (message != NULL_PTR) {
	currentCharacter = (short *)message;
	while (*(currentCharacter++) != 0)
	    length++;
    }

    jmessage = (*env)->NewString(env, message, length);

    jConstructor = (*env)->GetMethodID(env, jIOExceptionClass, "<init>", "(Ljava/lang/String;)V");
    assert(jConstructor != 0);
    jIOException = (jthrowable) (*env)->NewObject(env, jIOExceptionClass, jConstructor, jmessage);
    (*env)->Throw(env, jIOException);
}

/*
 * This function simply throws a PKCS#11RuntimeException with the given
 * string as its message. If the message is NULL_PTR, the exception is created
 * using the default constructor.
 *
 * @param env Used to call JNI functions and to get the Exception class.
 * @param jmessage The message string of the Exception object.
 */
void throwPKCS11RuntimeException(JNIEnv * env, jstring jmessage)
{
    jclass jPKCS11RuntimeExceptionClass;
    jmethodID jConstructor;
    jthrowable jPKCS11RuntimeException;

    jPKCS11RuntimeExceptionClass = (*env)->FindClass(env, CLASS_PKCS11RUNTIMEEXCEPTION);
    assert(jPKCS11RuntimeExceptionClass != 0);

    if (jmessage == NULL_PTR) {
	jConstructor = (*env)->GetMethodID(env, jPKCS11RuntimeExceptionClass, "<init>", "()V");
	assert(jConstructor != 0);
	jPKCS11RuntimeException = (jthrowable) (*env)->NewObject(env, jPKCS11RuntimeExceptionClass, jConstructor);
	(*env)->Throw(env, jPKCS11RuntimeException);
    } else {
	jConstructor = (*env)->GetMethodID(env, jPKCS11RuntimeExceptionClass, "<init>", "(Ljava/lang/String;)V");
	assert(jConstructor != 0);
	jPKCS11RuntimeException =
	    (jthrowable) (*env)->NewObject(env, jPKCS11RuntimeExceptionClass, jConstructor, jmessage);
	(*env)->Throw(env, jPKCS11RuntimeException);
    }
}

/*
 * This function simply throws a PKCS#11RuntimeException. The message says that
 * the object is not connected to the module.
 *
 * @param env Used to call JNI functions and to get the Exception class.
 */
void throwDisconnectedRuntimeException(JNIEnv * env)
{
    jstring jExceptionMessage = (*env)->NewStringUTF(env, "This object is not connected to a module.");

    throwPKCS11RuntimeException(env, jExceptionMessage);
}
