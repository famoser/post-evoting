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
/* for handling dual-function methods                                         */
/* ************************************************************************** */

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DigestEncryptUpdate
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG ulPartLen
 * @return  jbyteArray jEncryptedPart   CK_BYTE_PTR pEncryptedPart
 *                                      CK_ULONG_PTR pulEncryptedPartLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DigestEncryptUpdate
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jPart) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpPart = NULL_PTR, ckpEncryptedPart;
    CK_ULONG ckPartLength, ckEncryptedPartLength = 0;
    jbyteArray jEncryptedPart;
    CK_RV rv;
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
    if (jByteArrayToCKByteArray(env, jPart, &ckpPart, &ckPartLength)) {
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_DigestEncryptUpdate) (ckSessionHandle, ckpPart, ckPartLength, NULL_PTR,
						 &ckEncryptedPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpEncryptedPart = (CK_BYTE_PTR) malloc(ckEncryptedPartLength * sizeof(CK_BYTE));
    if (ckpEncryptedPart == NULL_PTR && ckEncryptedPartLength != 0) {
	free(ckpPart);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_DigestEncryptUpdate) (ckSessionHandle, ckpPart, ckPartLength, ckpEncryptedPart,
						 &ckEncryptedPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jEncryptedPart = ckByteArrayToJByteArray(env, ckpEncryptedPart, ckEncryptedPartLength);
    else
	jEncryptedPart = NULL_PTR;

    free(ckpPart);
    free(ckpEncryptedPart);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jEncryptedPart;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DecryptDigestUpdate
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jEncryptedPart   CK_BYTE_PTR pEncryptedPart
 *                                      CK_ULONG ulEncryptedPartLen
 * @return  jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG_PTR pulPartLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DecryptDigestUpdate
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jEncryptedPart) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpPart, ckpEncryptedPart = NULL_PTR;
    CK_ULONG ckPartLength = 0, ckEncryptedPartLength;
    jbyteArray jPart;
    CK_RV rv;
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
    if (jByteArrayToCKByteArray(env, jEncryptedPart, &ckpEncryptedPart, &ckEncryptedPartLength)) {
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_DecryptDigestUpdate) (ckSessionHandle, ckpEncryptedPart, ckEncryptedPartLength, NULL_PTR,
						 &ckPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpPart = (CK_BYTE_PTR) malloc(ckPartLength * sizeof(CK_BYTE));
    if (ckpPart == NULL_PTR && ckPartLength != 0) {
	free(ckpEncryptedPart);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_DecryptDigestUpdate) (ckSessionHandle, ckpEncryptedPart, ckEncryptedPartLength, ckpPart,
						 &ckPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jPart = ckByteArrayToJByteArray(env, ckpPart, ckPartLength);
    else
	jPart = NULL_PTR;

    free(ckpPart);
    free(ckpEncryptedPart);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jPart;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_SignEncryptUpdate
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG ulPartLen
 * @return  jbyteArray jEncryptedPart   CK_BYTE_PTR pEncryptedPart
 *                                      CK_ULONG_PTR pulEncryptedPartLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1SignEncryptUpdate
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jPart) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpPart = NULL_PTR, ckpEncryptedPart;
    CK_ULONG ckPartLength, ckEncryptedPartLength = 0;
    jbyteArray jEncryptedPart;
    CK_RV rv;
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
    if (jByteArrayToCKByteArray(env, jPart, &ckpPart, &ckPartLength)) {
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_SignEncryptUpdate) (ckSessionHandle, ckpPart, ckPartLength, NULL_PTR,
					       &ckEncryptedPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpEncryptedPart = (CK_BYTE_PTR) malloc(ckEncryptedPartLength * sizeof(CK_BYTE));
    if (ckpEncryptedPart == NULL_PTR && ckEncryptedPartLength != 0) {
	free(ckpPart);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_SignEncryptUpdate) (ckSessionHandle, ckpPart, ckPartLength, ckpEncryptedPart,
					       &ckEncryptedPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jEncryptedPart = ckByteArrayToJByteArray(env, ckpEncryptedPart, ckEncryptedPartLength);
    else
	jEncryptedPart = NULL_PTR;

    free(ckpPart);
    free(ckpEncryptedPart);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jEncryptedPart;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DecryptVerifyUpdate
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jEncryptedPart   CK_BYTE_PTR pEncryptedPart
 *                                      CK_ULONG ulEncryptedPartLen
 * @return  jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG_PTR pulPartLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DecryptVerifyUpdate
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jEncryptedPart) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpPart, ckpEncryptedPart = NULL_PTR;
    CK_ULONG ckPartLength = 0, ckEncryptedPartLength;
    jbyteArray jPart;
    CK_RV rv;
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
    if (jByteArrayToCKByteArray(env, jEncryptedPart, &ckpEncryptedPart, &ckEncryptedPartLength)) {
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_DecryptVerifyUpdate) (ckSessionHandle, ckpEncryptedPart, ckEncryptedPartLength, NULL_PTR,
						 &ckPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpPart = (CK_BYTE_PTR) malloc(ckPartLength * sizeof(CK_BYTE));
    if (ckpPart == NULL_PTR && ckPartLength != 0) {
	free(ckpEncryptedPart);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_DecryptVerifyUpdate) (ckSessionHandle, ckpEncryptedPart, ckEncryptedPartLength, ckpPart,
						 &ckPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jPart = ckByteArrayToJByteArray(env, ckpPart, ckPartLength);
    else
	jPart = NULL_PTR;

    free(ckpPart);
    free(ckpEncryptedPart);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jPart;
}
