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
/* for creating and verifying signatures and MACs                             */
/* ************************************************************************** */

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_SignInit
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;JZ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @return  jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1SignInit
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jKeyHandle, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM ckMechanism;
    CK_OBJECT_HANDLE ckKeyHandle;
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
    ckKeyHandle = jLongToCKULong(jKeyHandle);

    TRACE1(tag_call, __FUNCTION__, "calling HSM %ld", ckKeyHandle);
    rv = (*ckpFunctions->C_SignInit) (ckSessionHandle, &ckMechanism, ckKeyHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_Sign
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 * @return  jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG_PTR pulSignatureLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1Sign
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jData) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpData = NULL_PTR;
    CK_BYTE_PTR ckpSignature;
    CK_ULONG ckDataLength;
    CK_ULONG ckSignatureLength = 0;
    jbyteArray jSignature;
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
    jByteArrayToCKByteArray(env, jData, &ckpData, &ckDataLength);

    TRACE0(tag_call, __FUNCTION__, "getting necessary buffer length  C_SIGN");
    rv = (*ckpFunctions->C_Sign) (ckSessionHandle, ckpData, ckDataLength, NULL, &ckSignatureLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
        TRACE1(tag_debug, __FUNCTION__, "Failed to get necessary buffer lengths. RV: ", rv);
        free(ckpData);
        return NULL_PTR;
    }

    TRACE1(tag_debug, __FUNCTION__, "necessary buffer length  C_SIGN: %ld", ckDataLength);
    ckpSignature = (CK_BYTE_PTR) malloc(ckSignatureLength * sizeof(CK_BYTE));
    if (ckpSignature == NULL_PTR && ckSignatureLength != 0) {
        free(ckpData);
        throwOutOfMemoryError(env);
        return NULL_PTR;
    }
    TRACE0(tag_call, __FUNCTION__, "calling C_SIGN");
    rv = (*ckpFunctions->C_Sign) (ckSessionHandle, ckpData, ckDataLength, ckpSignature, &ckSignatureLength);
    TRACE0(tag_call, __FUNCTION__, "finished C_SIGN");

    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK) {
        TRACE1(tag_debug, __FUNCTION__, "rv is OK: ", rv);
        jSignature = ckByteArrayToJByteArray(env, ckpSignature, ckSignatureLength);
    } else {
        TRACE1(tag_debug, __FUNCTION__, "rv is not OK: ", rv);
        jSignature = NULL_PTR;
    }

    free(ckpData);
    free(ckpSignature);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jSignature;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_SignUpdate
 * Signature: (J[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG ulPartLen
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1SignUpdate
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
    if (jByteArrayToCKByteArray(env, jPart, &ckpPart, &ckPartLength)) {
	return;
    }

    rv = (*ckpFunctions->C_SignUpdate) (ckSessionHandle, ckpPart, ckPartLength);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    free(ckpPart);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_SignFinal
 * Signature: (J)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @return  jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG_PTR pulSignatureLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1SignFinal
    (JNIEnv * env, jobject obj, jlong jSessionHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpSignature;
    CK_ULONG ckSignatureLength = 0;
    jbyteArray jSignature;
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

    /* first determine the length of the signature */
    rv = (*ckpFunctions->C_SignFinal) (ckSessionHandle, NULL_PTR, &ckSignatureLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpSignature = (CK_BYTE_PTR) malloc(ckSignatureLength * sizeof(CK_BYTE));
    if (ckpSignature == NULL_PTR && ckSignatureLength != 0) {
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    /* now get the signature */
    rv = (*ckpFunctions->C_SignFinal) (ckSessionHandle, ckpSignature, &ckSignatureLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jSignature = ckByteArrayToJByteArray(env, ckpSignature, ckSignatureLength);
    else
	jSignature = NULL_PTR;

    free(ckpSignature);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jSignature;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_SignRecoverInit
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;JZ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @return  jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1SignRecoverInit
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jKeyHandle, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM ckMechanism;
    CK_OBJECT_HANDLE ckKeyHandle;
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
    ckKeyHandle = jLongToCKULong(jKeyHandle);

    rv = (*ckpFunctions->C_SignRecoverInit) (ckSessionHandle, &ckMechanism, ckKeyHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_SignRecover
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 * @return  jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG_PTR pulSignatureLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1SignRecover
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jData) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpData = NULL_PTR;
    CK_BYTE_PTR ckpSignature;
    CK_ULONG ckDataLength;
    CK_ULONG ckSignatureLength = 0;
    jbyteArray jSignature;
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
    if (jByteArrayToCKByteArray(env, jData, &ckpData, &ckDataLength)) {
	return NULL_PTR;
    }

    /* first determine the length of the signature */
    rv = (*ckpFunctions->C_SignRecover) (ckSessionHandle, ckpData, ckDataLength, NULL_PTR, &ckSignatureLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpSignature = (CK_BYTE_PTR) malloc(ckSignatureLength * sizeof(CK_BYTE));
    if (ckpSignature == NULL_PTR && ckSignatureLength != 0) {
	free(ckpData);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    /* now get the signature */
    rv = (*ckpFunctions->C_SignRecover) (ckSessionHandle, ckpData, ckDataLength, ckpSignature, &ckSignatureLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jSignature = ckByteArrayToJByteArray(env, ckpSignature, ckSignatureLength);
    else
	jSignature = NULL_PTR;

    free(ckpData);
    free(ckpSignature);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jSignature;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_VerifyInit
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;JZ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @return  jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1VerifyInit
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jKeyHandle, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM ckMechanism;
    CK_OBJECT_HANDLE ckKeyHandle;
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
    ckKeyHandle = jLongToCKULong(jKeyHandle);

    rv = (*ckpFunctions->C_VerifyInit) (ckSessionHandle, &ckMechanism, ckKeyHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_Verify
 * Signature: (J[B[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 * @param   jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG_PTR pulSignatureLen
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1Verify
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jData, jbyteArray jSignature) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpData = NULL_PTR;
    CK_BYTE_PTR ckpSignature = NULL_PTR;
    CK_ULONG ckDataLength;
    CK_ULONG ckSignatureLength;
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
    if (jByteArrayToCKByteArray(env, jData, &ckpData, &ckDataLength)) {
	return;
    }
    if (jByteArrayToCKByteArray(env, jSignature, &ckpSignature, &ckSignatureLength)) {
	return;
    }

    /* verify the signature */
    rv = (*ckpFunctions->C_Verify) (ckSessionHandle, ckpData, ckDataLength, ckpSignature, ckSignatureLength);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    free(ckpData);
    free(ckpSignature);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_VerifyUpdate
 * Signature: (J[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG ulPartLen
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1VerifyUpdate
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
    if (jByteArrayToCKByteArray(env, jPart, &ckpPart, &ckPartLength)) {
	return;
    }

    rv = (*ckpFunctions->C_VerifyUpdate) (ckSessionHandle, ckpPart, ckPartLength);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    free(ckpPart);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_VerifyFinal
 * Signature: (J[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG ulSignatureLen
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1VerifyFinal
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jSignature) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpSignature = NULL_PTR;
    CK_ULONG ckSignatureLength;
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
    if (jByteArrayToCKByteArray(env, jSignature, &ckpSignature, &ckSignatureLength)) {
	return;
    }

    /* verify the signature */
    rv = (*ckpFunctions->C_VerifyFinal) (ckSessionHandle, ckpSignature, ckSignatureLength);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    free(ckpSignature);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_VerifyRecoverInit
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;JZ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @return  jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1VerifyRecoverInit
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jKeyHandle, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM ckMechanism;
    CK_OBJECT_HANDLE ckKeyHandle;
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
    ckKeyHandle = jLongToCKULong(jKeyHandle);

    rv = (*ckpFunctions->C_VerifyRecoverInit) (ckSessionHandle, &ckMechanism, ckKeyHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_VerifyRecover
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jSignature       CK_BYTE_PTR pSignature
 *                                      CK_ULONG ulSignatureLen
 * @return  jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG_PTR pulDataLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1VerifyRecover
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jSignature) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpData;
    CK_BYTE_PTR ckpSignature = NULL_PTR;
    CK_ULONG ckDataLength = 0;
    CK_ULONG ckSignatureLength;
    jbyteArray jData;
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
    if (jByteArrayToCKByteArray(env, jSignature, &ckpSignature, &ckSignatureLength)) {
	return NULL_PTR;
    }

    /* first determine the length of the signature */
    rv = (*ckpFunctions->C_VerifyRecover) (ckSessionHandle, ckpSignature, ckSignatureLength, NULL_PTR, &ckDataLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpData = (CK_BYTE_PTR) malloc(ckDataLength * sizeof(CK_BYTE));
    if (ckpData == NULL_PTR && ckDataLength != 0) {
	free(ckpSignature);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    /* now get the signature */
    rv = (*ckpFunctions->C_VerifyRecover) (ckSessionHandle, ckpSignature, ckSignatureLength, ckpData, &ckDataLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jData = ckByteArrayToJByteArray(env, ckpData, ckDataLength);
    else
	jData = NULL_PTR;

    free(ckpData);
    free(ckpSignature);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jData;
}
