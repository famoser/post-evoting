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
/* for handling message digesting functions                                   */
/* ************************************************************************** */

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DigestInit
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;Z)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DigestInit
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobject jMechanism, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM ckMechanism;
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
    ckMechanism = jMechanismToCKMechanism(env, jMechanism, jUseUtf8);

    rv = (*ckpFunctions->C_DigestInit) (ckSessionHandle, &ckMechanism);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_Digest
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 * @return  jbyteArray jDigest          CK_BYTE_PTR pDigest
 *                                      CK_ULONG_PTR pulDigestLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1Digest
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jData) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpData = NULL_PTR, ckpDigest;
    CK_ULONG ckDataLength, ckDigestLength = 0;
    jbyteArray jDigest;
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

    /* convert jTypes to ckTypes */
    ckSessionHandle = jLongToCKULong(jSessionHandle);
    if (jByteArrayToCKByteArray(env, jData, &ckpData, &ckDataLength)) {
	return NULL_PTR;
    }

    /* call C_Encrypt to determine DataLength */
    rv = (*ckpFunctions->C_Digest) (ckSessionHandle, ckpData, ckDataLength, NULL_PTR, &ckDigestLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    /* allocate memory for Data */
    ckpDigest = (CK_BYTE_PTR) malloc(ckDigestLength * sizeof(CK_BYTE));
    if (ckpDigest == NULL_PTR && ckDigestLength != 0) {
	free(ckpDigest);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    /* call C_Encrypt */
    rv = (*ckpFunctions->C_Digest) (ckSessionHandle, ckpData, ckDataLength, ckpDigest, &ckDigestLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	/* convert ckTypes to jTypes */
	jDigest = ckByteArrayToJByteArray(env, ckpDigest, ckDigestLength);
    else
	jDigest = NULL_PTR;

    free(ckpData);
    free(ckpDigest);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jDigest;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DigestUpdate
 * Signature: (J[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DigestUpdate
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jPart) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpPart = NULL_PTR;
    CK_ULONG ckPartLength;
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

    jByteArrayToCKByteArray(env, jPart, &ckpPart, &ckPartLength);

    rv = (*ckpFunctions->C_DigestUpdate) (ckSessionHandle, ckpPart, ckPartLength);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    free(ckpPart);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DigestKey
 * Signature: (JJ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DigestKey
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jlong jKeyHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_ULONG ckKeyHandle;
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
    ckKeyHandle = jLongToCKULong(jKeyHandle);

    rv = (*ckpFunctions->C_DigestKey) (ckSessionHandle, ckKeyHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DigestFinal
 * Signature: (J)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @return  jbyteArray jDigest          CK_BYTE_PTR pDigest
 *                                      CK_ULONG_PTR pulDigestLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DigestFinal
    (JNIEnv * env, jobject obj, jlong jSessionHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpDigest;
    CK_ULONG ckDigestLength = 0;
    jbyteArray jDigest;
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

    rv = (*ckpFunctions->C_DigestFinal) (ckSessionHandle, NULL_PTR, &ckDigestLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpDigest = (CK_BYTE_PTR) malloc(ckDigestLength * sizeof(CK_BYTE));
    if (ckpDigest == NULL_PTR && ckDigestLength != 0) {
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_DigestFinal) (ckSessionHandle, ckpDigest, &ckDigestLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jDigest = ckByteArrayToJByteArray(env, ckpDigest, ckDigestLength);
    else
	jDigest = NULL_PTR;

    free(ckpDigest);
    TRACE0(tag_call, __FUNCTION__, "exiting ");

    return jDigest;
}
