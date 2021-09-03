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
/* for handling encryption and decryption related functions                   */
/* ************************************************************************** */

CK_ULONG addLengthDecrypt = 31;

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_EncryptInit
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;JZ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1EncryptInit
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
    ckKeyHandle = jLongToCKULong(jKeyHandle);
    ckMechanism = jMechanismToCKMechanism(env, jMechanism, jUseUtf8);

    rv = (*ckpFunctions->C_EncryptInit) (ckSessionHandle, &ckMechanism, ckKeyHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_Encrypt
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG ulDataLen
 * @return  jbyteArray jEncryptedData   CK_BYTE_PTR pEncryptedData
 *                                      CK_ULONG_PTR pulEncryptedDataLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1Encrypt
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jData) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpData = NULL_PTR, ckpEncryptedData;
    CK_ULONG ckDataLength, ckEncryptedDataLength = 0;
    jbyteArray jEncryptedData;
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
    rv = (*ckpFunctions->C_Encrypt) (ckSessionHandle, ckpData, ckDataLength, NULL_PTR, &ckEncryptedDataLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    /* allocate memory for Data */
    ckpEncryptedData = (CK_BYTE_PTR) malloc(ckEncryptedDataLength * sizeof(CK_BYTE));
    if (ckpEncryptedData == NULL_PTR && ckEncryptedDataLength != 0) {
	free(ckpEncryptedData);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    /* call C_Encrypt */
    rv = (*ckpFunctions->C_Encrypt) (ckSessionHandle, ckpData, ckDataLength, ckpEncryptedData, &ckEncryptedDataLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	/* convert ckTypes to jTypes */
	jEncryptedData = ckByteArrayToJByteArray(env, ckpEncryptedData, ckEncryptedDataLength);
    else
	jEncryptedData = NULL_PTR;

    free(ckpData);
    free(ckpEncryptedData);

    TRACE0(tag_call, __FUNCTION__, "exiting ");

    return jEncryptedData;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_EncryptUpdate
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG ulPartLen
 * @return  jbyteArray jEncryptedPart   CK_BYTE_PTR pEncryptedPart
 *                                      CK_ULONG_PTR pulEncryptedPartLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1EncryptUpdate
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

    rv = (*ckpFunctions->C_EncryptUpdate) (ckSessionHandle, ckpPart, ckPartLength, NULL_PTR, &ckEncryptedPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpEncryptedPart = (CK_BYTE_PTR) malloc(ckEncryptedPartLength * sizeof(CK_BYTE));
    if (ckpEncryptedPart == NULL_PTR && ckEncryptedPartLength != 0) {
	free(ckpEncryptedPart);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_EncryptUpdate) (ckSessionHandle, ckpPart, ckPartLength, ckpEncryptedPart,
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
 * Method:    C_EncryptFinal
 * Signature: (J)[B
 * Parametermapping:                        *PKCS11*
 * @param   jlong jSessionHandle            CK_SESSION_HANDLE hSession
 * @return  jbyteArray jLastEncryptedPart   CK_BYTE_PTR pLastEncryptedDataPart
 *                                          CK_ULONG_PTR pulLastEncryptedDataPartLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1EncryptFinal
    (JNIEnv * env, jobject obj, jlong jSessionHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpLastEncryptedPart;
    CK_ULONG ckLastEncryptedPartLength = 0;
    jbyteArray jLastEncryptedPart;
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

    rv = (*ckpFunctions->C_EncryptFinal) (ckSessionHandle, NULL_PTR, &ckLastEncryptedPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpLastEncryptedPart = (CK_BYTE_PTR) malloc(ckLastEncryptedPartLength * sizeof(CK_BYTE));
    if (ckpLastEncryptedPart == NULL_PTR && ckLastEncryptedPartLength != 0) {
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_EncryptFinal) (ckSessionHandle, ckpLastEncryptedPart, &ckLastEncryptedPartLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jLastEncryptedPart = ckByteArrayToJByteArray(env, ckpLastEncryptedPart, ckLastEncryptedPartLength);
    else
	jLastEncryptedPart = NULL_PTR;

    free(ckpLastEncryptedPart);

    TRACE0(tag_call, __FUNCTION__, "exiting ");

    return jLastEncryptedPart;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DecryptInit
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;JZ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DecryptInit
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
    ckKeyHandle = jLongToCKULong(jKeyHandle);
    ckMechanism = jMechanismToCKMechanism(env, jMechanism, jUseUtf8);

    rv = (*ckpFunctions->C_DecryptInit) (ckSessionHandle, &ckMechanism, ckKeyHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_Decrypt
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jEncryptedData   CK_BYTE_PTR pEncryptedData
 *                                      CK_ULONG ulEncryptedDataLen
 * @return  jbyteArray jData            CK_BYTE_PTR pData
 *                                      CK_ULONG_PTR pulDataLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1Decrypt
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jEncryptedData) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpData, ckpDataTmp, ckpEncryptedData = NULL_PTR;
    CK_ULONG ckDataLength, ckEncryptedDataLength;
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

    /* convert jTypes to ckTypes */
    ckSessionHandle = jLongToCKULong(jSessionHandle);
    if (jByteArrayToCKByteArray(env, jEncryptedData, &ckpEncryptedData, &ckEncryptedDataLength)) {
	return NULL_PTR;
    }

    ckDataLength = ckEncryptedDataLength + addLengthDecrypt;

//      /* call C_Decrypt to determine DataLength */
//      rv = (*ckpFunctions->C_Decrypt)(ckSessionHandle, ckpEncryptedData, ckEncryptedDataLength, NULL_PTR, &ckDataLength);
//      if(ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) { return NULL_PTR; }

    /* allocate memory for Data */
    ckpData = (CK_BYTE_PTR) malloc(ckDataLength * sizeof(CK_BYTE));
    if (ckpData == NULL_PTR && ckDataLength != 0) {
	free(ckpEncryptedData);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    /* call C_Decrypt */
    rv = (*ckpFunctions->C_Decrypt) (ckSessionHandle, ckpEncryptedData, ckEncryptedDataLength, ckpData, &ckDataLength);
    if (rv == CKR_BUFFER_TOO_SMALL) {
        TRACE0(tag_debug, __FUNCTION__, "buffer too small, try again");
        addLengthDecrypt = ckDataLength - ckEncryptedDataLength;

        ckpDataTmp = (CK_BYTE_PTR) realloc(ckpData, ckDataLength * sizeof(CK_BYTE));
        if (ckpDataTmp == NULL_PTR && ckDataLength != 0) {
            free(ckpEncryptedData);
            free(ckpData);
            throwOutOfMemoryError(env);
            return NULL_PTR;
        }
        ckpData = ckpDataTmp;
        /* call C_Decrypt again */
        rv = (*ckpFunctions->C_Decrypt) (ckSessionHandle, ckpEncryptedData, ckEncryptedDataLength, ckpData,
                         &ckDataLength);

        if (rv == CKR_BUFFER_TOO_SMALL) {
            TRACE0(tag_debug, __FUNCTION__, "buffer too small again, try again");
            rv = (*ckpFunctions->C_Decrypt) (ckSessionHandle, ckpEncryptedData, ckEncryptedDataLength, NULL,
                             &ckDataLength);
            ckpDataTmp = (CK_BYTE_PTR) realloc(ckpData, ckDataLength * sizeof(CK_BYTE));
            if (ckpDataTmp == NULL_PTR && ckDataLength != 0) {
            free(ckpEncryptedData);
            free(ckpData);
            throwOutOfMemoryError(env);
            return NULL_PTR;
            }
            ckpData = ckpDataTmp;
            rv = (*ckpFunctions->C_Decrypt) (ckSessionHandle, ckpEncryptedData, ckEncryptedDataLength, ckpData,
                             &ckDataLength);
        }
    }

    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	/* convert ckTypes to jTypes */
	jData = ckByteArrayToJByteArray(env, ckpData, ckDataLength);
    else
	jData = NULL_PTR;

    free(ckpData);
    free(ckpEncryptedData);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jData;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DecryptUpdate
 * Signature: (J[B)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jEncryptedPart   CK_BYTE_PTR pEncryptedPart
 *                                      CK_ULONG ulEncryptedPartLen
 * @return  jbyteArray jPart            CK_BYTE_PTR pPart
 *                                      CK_ULONG_PTR pulPartLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DecryptUpdate
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jEncryptedPart) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpPart, ckpPartTmp, ckpEncryptedPart = NULL_PTR;
    CK_ULONG ckPartLength, ckEncryptedPartLength;
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
//      rv = (*ckpFunctions->C_DecryptUpdate)(ckSessionHandle, ckpEncryptedPart, ckEncryptedPartLength, NULL_PTR, &ckPartLength);
//      if(ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) { return NULL_PTR ; }

    ckPartLength = ckEncryptedPartLength + addLengthDecrypt;

    ckpPart = (CK_BYTE_PTR) malloc(ckPartLength * sizeof(CK_BYTE));
    if (ckpPart == NULL_PTR && ckPartLength != 0) {
	free(ckpEncryptedPart);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_DecryptUpdate) (ckSessionHandle, ckpEncryptedPart, ckEncryptedPartLength, ckpPart,
					   &ckPartLength);
    if (rv == CKR_BUFFER_TOO_SMALL) {
        TRACE0(tag_debug, __FUNCTION__, "buffer too small, try again");
        addLengthDecrypt = ckPartLength - ckEncryptedPartLength;

        ckpPartTmp = (CK_BYTE_PTR) realloc(ckpPart, ckPartLength * sizeof(CK_BYTE));
        if (ckpPartTmp == NULL_PTR && ckPartLength != 0) {
            free(ckpEncryptedPart);
            free(ckpPart);
            throwOutOfMemoryError(env);
            return NULL_PTR;
        }
        ckpPart = ckpPartTmp;
        /* call C_DecryptUpdate again */
        rv = (*ckpFunctions->C_DecryptUpdate) (ckSessionHandle, ckpEncryptedPart, ckEncryptedPartLength, ckpPart,
                               &ckPartLength);
        if (rv == CKR_BUFFER_TOO_SMALL) {
            TRACE0(tag_debug, __FUNCTION__, "buffer too small again, try again");
            rv = (*ckpFunctions->C_DecryptUpdate) (ckSessionHandle, ckpEncryptedPart, ckEncryptedPartLength, NULL,
                               &ckPartLength);
            ckpPartTmp = (CK_BYTE_PTR) realloc(ckpPart, ckPartLength * sizeof(CK_BYTE));
            if (ckpPartTmp == NULL_PTR && ckPartLength != 0) {
            free(ckpEncryptedPart);
            free(ckpPart);
            throwOutOfMemoryError(env);
            return NULL_PTR;
            }
            ckpPart = ckpPartTmp;
        rv = (*ckpFunctions->C_DecryptUpdate) (ckSessionHandle, ckpEncryptedPart, ckEncryptedPartLength, ckpPart,
                               &ckPartLength);
        }
    }

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
 * Method:    C_DecryptFinal
 * Signature: (J)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @return  jbyteArray jLastPart        CK_BYTE_PTR pLastPart
 *                                      CK_ULONG_PTR pulLastPartLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DecryptFinal
    (JNIEnv * env, jobject obj, jlong jSessionHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpLastPart, ckpLastPartTmp;
    CK_ULONG ckLastPartLength = addLengthDecrypt;
    jbyteArray jLastPart;
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

//      rv = (*ckpFunctions->C_DecryptFinal)(ckSessionHandle, NULL_PTR, &ckLastPartLength);
//      if(ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) { return NULL_PTR ; }

    ckpLastPart = (CK_BYTE_PTR) malloc(ckLastPartLength * sizeof(CK_BYTE));
    if (ckpLastPart == NULL_PTR && ckLastPartLength != 0) {
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }
    rv = (*ckpFunctions->C_DecryptFinal) (ckSessionHandle, ckpLastPart, &ckLastPartLength);
    if (rv == CKR_BUFFER_TOO_SMALL) {
        TRACE0(tag_debug, __FUNCTION__, "buffer too small, try again");
        addLengthDecrypt = ckLastPartLength;

        ckpLastPartTmp = (CK_BYTE_PTR) realloc(ckpLastPart, ckLastPartLength * sizeof(CK_BYTE));
        if (ckpLastPartTmp == NULL_PTR && ckLastPartLength != 0) {
            free(ckpLastPart);
            throwOutOfMemoryError(env);
            return NULL_PTR;
        }
        ckpLastPart = ckpLastPartTmp;
        /* call C_DecryptFinal again */
        rv = (*ckpFunctions->C_DecryptFinal) (ckSessionHandle, ckpLastPart, &ckLastPartLength);
        if (rv == CKR_BUFFER_TOO_SMALL) {
            TRACE0(tag_debug, __FUNCTION__, "buffer too small again, try again");
            rv = (*ckpFunctions->C_DecryptFinal) (ckSessionHandle, NULL, &ckLastPartLength);
            ckpLastPartTmp = (CK_BYTE_PTR) realloc(ckpLastPart, ckLastPartLength * sizeof(CK_BYTE));
            if (ckpLastPartTmp == NULL_PTR && ckLastPartLength != 0) {
            free(ckpLastPart);
            throwOutOfMemoryError(env);
            return NULL_PTR;
            }
            ckpLastPart = ckpLastPartTmp;
            rv = (*ckpFunctions->C_DecryptFinal) (ckSessionHandle, ckpLastPart, &ckLastPartLength);
        }
    }

    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jLastPart = ckByteArrayToJByteArray(env, ckpLastPart, ckLastPartLength);
    else
	jLastPart = NULL_PTR;

    free(ckpLastPart);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jLastPart;
}
