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
/* for creating, deriving or (un)wrapping keys                                */
/* ************************************************************************** */

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GenerateKey
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;[Liaik/pkcs/pkcs11/wrapper/CK_ATTRIBUTE;Z)J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jobjectArray jTemplate      CK_ATTRIBUTE_PTR pTemplate
 *                                      CK_ULONG ulCount
 * @return  jlong jKeyHandle            CK_OBJECT_HANDLE_PTR phKey
 */
JNIEXPORT jlong JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GenerateKey
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobject jMechanism, jobjectArray jTemplate, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM ckMechanism;
    CK_ATTRIBUTE_PTR ckpAttributes = NULL_PTR, ckAttributeArray;
    CK_ULONG ckAttributesLength;
    CK_OBJECT_HANDLE ckKeyHandle;
    jlong jKeyHandle;
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
    ckMechanism = jMechanismToCKMechanism(env, jMechanism, jUseUtf8);
    if ((*env)->ExceptionOccurred(env)) {
	return 0L;
    }
    if (jAttributeArrayToCKAttributeArray(env, jTemplate, &ckpAttributes, &ckAttributesLength, jUseUtf8)) {
	return 0L;
    }

    rv = (*ckpFunctions->C_GenerateKey) (ckSessionHandle, &ckMechanism, ckpAttributes, ckAttributesLength,
					 &ckKeyHandle);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jKeyHandle = ckULongToJLong(ckKeyHandle);
    else
	jKeyHandle = 0L;

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

    /* check, if we must give a initialization vector back to Java */
    switch (ckMechanism.mechanism) {
    case CKM_PBE_MD2_DES_CBC:
    case CKM_PBE_MD5_DES_CBC:
    case CKM_PBE_MD5_CAST_CBC:
    case CKM_PBE_MD5_CAST3_CBC:
    case CKM_PBE_MD5_CAST128_CBC:
	/* case CKM_PBE_MD5_CAST5_CBC:  the same as CKM_PBE_MD5_CAST128_CBC */
    case CKM_PBE_SHA1_CAST128_CBC:
	/* case CKM_PBE_SHA1_CAST5_CBC: the same as CKM_PBE_SHA1_CAST128_CBC */
	/* we must copy back the initialization vector to the jMechanism object */
	copyBackPBEInitializationVector(env, &ckMechanism, jMechanism);
	break;
    }

    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jKeyHandle;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GenerateKeyPair
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;[Liaik/pkcs/pkcs11/wrapper/CK_ATTRIBUTE;[Liaik/pkcs/pkcs11/wrapper/CK_ATTRIBUTE;Z)[J
 * Parametermapping:                          *PKCS11*
 * @param   jlong jSessionHandle              CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism                CK_MECHANISM_PTR pMechanism
 * @param   jobjectArray jPublicKeyTemplate   CK_ATTRIBUTE_PTR pPublicKeyTemplate
 *                                            CK_ULONG ulPublicKeyAttributeCount
 * @param   jobjectArray jPrivateKeyTemplate  CK_ATTRIBUTE_PTR pPrivateKeyTemplate
 *                                            CK_ULONG ulPrivateKeyAttributeCount
 * @return  jlongArray jKeyHandles            CK_OBJECT_HANDLE_PTR phPublicKey
 *                                            CK_OBJECT_HANDLE_PTR phPublicKey
 */
JNIEXPORT jlongArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GenerateKeyPair
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobject jMechanism,
     jobjectArray jPublicKeyTemplate, jobjectArray jPrivateKeyTemplate, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM ckMechanism;
    CK_ATTRIBUTE_PTR ckpPublicKeyAttributes = NULL_PTR;
    CK_ATTRIBUTE_PTR ckpPrivateKeyAttributes = NULL_PTR;
    CK_ATTRIBUTE_PTR ckAttributeArray;
    CK_ULONG ckPublicKeyAttributesLength;
    CK_ULONG ckPrivateKeyAttributesLength;
    CK_OBJECT_HANDLE_PTR ckpPublicKeyHandle;	/* pointer to Public Key */
    CK_OBJECT_HANDLE_PTR ckpPrivateKeyHandle;	/* pointer to Private Key */
    CK_OBJECT_HANDLE_PTR ckpKeyHandles;	/* pointer to array with Public and Private Key */
    CK_ULONG i, j, length;
    jlongArray jKeyHandles;
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
    ckMechanism = jMechanismToCKMechanism(env, jMechanism, jUseUtf8);
    if (jAttributeArrayToCKAttributeArray
	(env, jPublicKeyTemplate, &ckpPublicKeyAttributes, &ckPublicKeyAttributesLength, jUseUtf8)) {
	return NULL_PTR;
    }
    if (jAttributeArrayToCKAttributeArray
	(env, jPrivateKeyTemplate, &ckpPrivateKeyAttributes, &ckPrivateKeyAttributesLength, jUseUtf8)) {
	return NULL_PTR;
    }
    ckpKeyHandles = (CK_OBJECT_HANDLE_PTR) malloc(2 * sizeof(CK_OBJECT_HANDLE));
    if (ckpKeyHandles == NULL_PTR) {
	free(ckpPublicKeyAttributes);
	free(ckpPrivateKeyAttributes);
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }
    ckpPublicKeyHandle = ckpKeyHandles;	/* first element of array is Public Key */
    ckpPrivateKeyHandle = (ckpKeyHandles + 1);	/* second element of array is Private Key */

    rv = (*ckpFunctions->C_GenerateKeyPair) (ckSessionHandle, &ckMechanism,
					     ckpPublicKeyAttributes, ckPublicKeyAttributesLength,
					     ckpPrivateKeyAttributes, ckPrivateKeyAttributesLength,
					     ckpPublicKeyHandle, ckpPrivateKeyHandle);

    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jKeyHandles = ckULongArrayToJLongArray(env, ckpKeyHandles, 2);
    else
	jKeyHandles = NULL_PTR;

    for (i = 0; i < ckPublicKeyAttributesLength; i++) {
	if (ckpPublicKeyAttributes[i].pValue != NULL_PTR) {
	    if ((ckpPublicKeyAttributes[i].type == 0x40000211) || (ckpPublicKeyAttributes[i].type == 0x40000212)) {
		ckAttributeArray = (CK_ATTRIBUTE_PTR) ckpPublicKeyAttributes[i].pValue;
		length = ckpPublicKeyAttributes[i].ulValueLen / sizeof(CK_ATTRIBUTE);
		for (j = 0; j < length; j++) {
		    free(ckAttributeArray[j].pValue);
		}
	    }
	    free(ckpPublicKeyAttributes[i].pValue);
	}
    }
    free(ckpPublicKeyAttributes);

    for (i = 0; i < ckPrivateKeyAttributesLength; i++) {
	if (ckpPrivateKeyAttributes[i].pValue != NULL_PTR) {
	    if ((ckpPrivateKeyAttributes[i].type == 0x40000211) || (ckpPrivateKeyAttributes[i].type == 0x40000212)) {
		ckAttributeArray = (CK_ATTRIBUTE_PTR) ckpPrivateKeyAttributes[i].pValue;
		length = ckpPrivateKeyAttributes[i].ulValueLen / sizeof(CK_ATTRIBUTE);
		for (j = 0; j < length; j++) {
		    free(ckAttributeArray[j].pValue);
		}
	    }
	    free(ckpPrivateKeyAttributes[i].pValue);
	}
    }
    free(ckpPrivateKeyAttributes);

    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    free(ckpKeyHandles);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jKeyHandles;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_WrapKey
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;JJZ)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jlong jWrappingKeyHandle    CK_OBJECT_HANDLE hWrappingKey
 * @param   jlong jKeyHandle            CK_OBJECT_HANDLE hKey
 * @return  jbyteArray jWrappedKey      CK_BYTE_PTR pWrappedKey
 *                                      CK_ULONG_PTR pulWrappedKeyLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1WrapKey
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jWrappingKeyHandle, jlong jKeyHandle,
     jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM ckMechanism;
    CK_OBJECT_HANDLE ckWrappingKeyHandle;
    CK_OBJECT_HANDLE ckKeyHandle;
    CK_BYTE_PTR ckpWrappedKey;
    CK_ULONG ckWrappedKeyLength = 0;
    jbyteArray jWrappedKey;
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
    ckMechanism = jMechanismToCKMechanism(env, jMechanism, jUseUtf8);
    ckWrappingKeyHandle = jLongToCKULong(jWrappingKeyHandle);
    ckKeyHandle = jLongToCKULong(jKeyHandle);

    rv = (*ckpFunctions->C_WrapKey) (ckSessionHandle, &ckMechanism, ckWrappingKeyHandle, ckKeyHandle, NULL_PTR,
				     &ckWrappedKeyLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpWrappedKey = (CK_BYTE_PTR) malloc(ckWrappedKeyLength * sizeof(CK_BYTE));
    if (ckpWrappedKey == NULL_PTR && ckWrappedKeyLength != 0) {
	if (ckMechanism.pParameter != NULL_PTR) {
	    free(ckMechanism.pParameter);
	}
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_WrapKey) (ckSessionHandle, &ckMechanism, ckWrappingKeyHandle, ckKeyHandle, ckpWrappedKey,
				     &ckWrappedKeyLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jWrappedKey = ckByteArrayToJByteArray(env, ckpWrappedKey, ckWrappedKeyLength);
    else
	jWrappedKey = NULL_PTR;

    free(ckpWrappedKey);
    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jWrappedKey;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_UnwrapKey
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;J[B[Liaik/pkcs/pkcs11/wrapper/CK_ATTRIBUTE;Z)J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jlong jUnwrappingKeyHandle  CK_OBJECT_HANDLE hUnwrappingKey
 * @param   jbyteArray jWrappedKey      CK_BYTE_PTR pWrappedKey
 *                                      CK_ULONG_PTR pulWrappedKeyLen
 * @param   jobjectArray jTemplate      CK_ATTRIBUTE_PTR pTemplate
 *                                      CK_ULONG ulCount
 * @return  jlong jKeyHandle            CK_OBJECT_HANDLE_PTR phKey
 */
JNIEXPORT jlong JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1UnwrapKey
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jUnwrappingKeyHandle,
     jbyteArray jWrappedKey, jobjectArray jTemplate, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM ckMechanism;
    CK_OBJECT_HANDLE ckUnwrappingKeyHandle;
    CK_BYTE_PTR ckpWrappedKey = NULL_PTR;
    CK_ULONG ckWrappedKeyLength;
    CK_ATTRIBUTE_PTR ckpAttributes = NULL_PTR, ckAttributeArray;
    CK_ULONG ckAttributesLength;
    CK_OBJECT_HANDLE ckKeyHandle;
    jlong jKeyHandle;
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
    ckMechanism = jMechanismToCKMechanism(env, jMechanism, jUseUtf8);
    ckUnwrappingKeyHandle = jLongToCKULong(jUnwrappingKeyHandle);
    if (jByteArrayToCKByteArray(env, jWrappedKey, &ckpWrappedKey, &ckWrappedKeyLength)) {
	return 0L;
    }
    if (jAttributeArrayToCKAttributeArray(env, jTemplate, &ckpAttributes, &ckAttributesLength, jUseUtf8)) {
	return 0L;
    }

    rv = (*ckpFunctions->C_UnwrapKey) (ckSessionHandle, &ckMechanism, ckUnwrappingKeyHandle,
				       ckpWrappedKey, ckWrappedKeyLength,
				       ckpAttributes, ckAttributesLength, &ckKeyHandle);

    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jKeyHandle = ckLongToJLong(ckKeyHandle);
    else
	jKeyHandle = 0L;

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

    /* check, if we must give a initialization vector back to Java */
    if (ckMechanism.mechanism == CKM_KEY_WRAP_SET_OAEP) {
	/* we must copy back the unwrapped key info to the jMechanism object */
	copyBackSetUnwrappedKey(env, &ckMechanism, jMechanism);
    }

    free(ckpWrappedKey);
    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jKeyHandle;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_DeriveKey
 * Signature: (JLiaik/pkcs/pkcs11/wrapper/CK_MECHANISM;J[Liaik/pkcs/pkcs11/wrapper/CK_ATTRIBUTE;Z)J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jobject jMechanism          CK_MECHANISM_PTR pMechanism
 * @param   jlong jBaseKeyHandle        CK_OBJECT_HANDLE hBaseKey
 * @param   jobjectArray jTemplate      CK_ATTRIBUTE_PTR pTemplate
 *                                      CK_ULONG ulCount
 * @return  jlong jKeyHandle            CK_OBJECT_HANDLE_PTR phKey
 */
JNIEXPORT jlong JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1DeriveKey
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jobject jMechanism, jlong jBaseKeyHandle, jobjectArray jTemplate,
     jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_MECHANISM ckMechanism;
    CK_OBJECT_HANDLE ckBaseKeyHandle;
    CK_ATTRIBUTE_PTR ckpAttributes = NULL_PTR, ckAttributeArray;
    CK_ULONG ckAttributesLength;
    CK_OBJECT_HANDLE ckKeyHandle;
    jlong jKeyHandle;
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
    ckMechanism = jMechanismToCKMechanism(env, jMechanism, jUseUtf8);
    ckBaseKeyHandle = jLongToCKULong(jBaseKeyHandle);
    if (jAttributeArrayToCKAttributeArray(env, jTemplate, &ckpAttributes, &ckAttributesLength, jUseUtf8)) {
	return 0L;
    }

    rv = (*ckpFunctions->C_DeriveKey) (ckSessionHandle, &ckMechanism, ckBaseKeyHandle,
				       ckpAttributes, ckAttributesLength, &ckKeyHandle);

    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK) {
	jKeyHandle = ckLongToJLong(ckKeyHandle);
	if (ckMechanism.mechanism == CKM_SSL3_MASTER_KEY_DERIVE) {
	    /* we must copy back the client version */
	    copyBackClientVersion(env, &ckMechanism, jMechanism);
	}
	if (ckMechanism.mechanism == CKM_SSL3_KEY_AND_MAC_DERIVE) {
	    /* we must copy back the unwrapped key info to the jMechanism object */
	    copyBackSSLKeyMatParams(env, &ckMechanism, jMechanism);
	}
    } else
	jKeyHandle = 0L;

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

    if (ckMechanism.pParameter != NULL_PTR) {
	freeCKMechanismParameter(&ckMechanism);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jKeyHandle;
}
