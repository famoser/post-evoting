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
    
/*
 * pkcs11wrapper.c
 * 18.05.2001
 *
 * This is the implementation of the native functions of the Java to PKCS#11 interface.
 * All function use some helper functions to convert the JNI types to PKCS#11 types.
 *
 * @author Karl Scheibelhofer
 * @author Martin Schlaeffer
 */ 
    
#include "pkcs11wrapper.h"
    
#include "dualfunction.c"
#include "encryption.c"
#include "getattributevalue.c"
#include "keymanagement.c"
#include "messagedigest.c"
#include "modules.c"
#include "objectmanagement.c"
#include "sessions.c"
#include "signature.c"
#include "slotsandtokens.c"
#include "util_conversion.c"
#include "util_conversion_algorithms.c"
#include "util_errorhandling.c"
    
#include "platform.c"
    
/* ************************************************************************** */ 
/* This file contains random generation functions, general and legacy pkcs#11 */ 
/* functions as well as some helper functions                                 */ 
/* ************************************************************************** */ 
     
/* ************************************************************************** */ 
/* Functions called by the VM when it loads or unloads this library           */ 
/* ************************************************************************** */ 
     
/*
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) 
{
  return JNI_VERSION_1_2 ;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{

}
*/ 
    
/* ************************************************************************** */ 
/* Helper functions                                                           */ 
/* ************************************************************************** */ 
    
/*
 * This method retrieves the function pointers from the module struct. Returns NULL_PTR
 * if either the module is NULL_PTR or the function pointer list is NULL_PTR. Returns the
 * function pointer list on success.
 */ 
    CK_FUNCTION_LIST_PTR getFunctionList(JNIEnv * env, ModuleData * moduleData)  { CK_FUNCTION_LIST_PTR ckpFunctions;
     ckpFunctions = moduleData->ckFunctionListPtr;
    if (ckpFunctions == NULL_PTR) {
	throwPKCS11RuntimeException(env, (*env)->NewStringUTF(env, "This modules does not provide methods"));
	return NULL_PTR;
    }
    return ckpFunctions;
}

 
/*
 * converts a given array of chars into a human readable hex string
 */ 
/*
void byteArrayToHexString(char* array, int array_length, char* result, int result_length)
{
	int i = 0;
	char lut[16] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	for(i; i < array_length; i++)
	{
		if(2 * i + 1 > result_length - 4) {
			result[2 * i] = '.';
			result[2 * i + 1] = '.';
			result[2 * i + 2] = '.';
			break;
		}

		result[2 * i] = lut[(array[i] & 0xF0) >> 4];
		result[2 * i + 1] = lut[array[i] & 0x0F];
	}
}
 */ 
    
/*
 * This function compares the two given objects using the equals method as
 * implemented by the Object class; i.e. it checks, if both references refer
 * to the same object. If both references are NULL_PTR, this functions also regards
 * them as equal.
 */ 
int equals(JNIEnv * env, jobject thisObject, jobject otherObject)
{
    jclass jObjectClass;
    jmethodID jequals;
    jboolean jequal = JNI_FALSE;
    int returnValue;
     if (thisObject != NULL_PTR) {
	jObjectClass = (*env)->FindClass(env, "java/lang/Object");
	assert(jObjectClass != 0);
	jequals = (*env)->GetMethodID(env, jObjectClass, "equals", "(Ljava/lang/Object;)Z");
	assert(jequals != 0);
	
	    /* We must call the equals method as implemented by the Object class. This
	     * method compares if both references refer to the same object. This is what
	     * we want.
	     */ 
	    jequal = (*env)->CallNonvirtualBooleanMethod(env, thisObject, jObjectClass, jequals, otherObject);
    } else if (otherObject == NULL_PTR) {
	jequal = JNI_TRUE;	/* both NULL_PTR, we regard equal */
    }
     returnValue = (jequal == JNI_TRUE) ? 1 : 0;
     return returnValue;
}

 void freeCKMechanismParameter(CK_MECHANISM_PTR mechanism)
{
    void *value;
     
	/* free pointers inside parameter structures, see jMechanismParameterToCKMechanismParameter */ 
	switch (mechanism->mechanism) {
    case CKM_RSA_PKCS_OAEP:
	value = ((CK_RSA_PKCS_OAEP_PARAMS_PTR) mechanism->pParameter)->pSourceData;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_KEA_KEY_DERIVE:
	value = ((CK_KEA_DERIVE_PARAMS_PTR) mechanism->pParameter)->pRandomA;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_KEA_DERIVE_PARAMS_PTR) mechanism->pParameter)->pRandomB;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_KEA_DERIVE_PARAMS_PTR) mechanism->pParameter)->pPublicData;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_RC5_CBC:
    case CKM_RC5_CBC_PAD:
	value = ((CK_RC5_CBC_PARAMS_PTR) mechanism->pParameter)->pIv;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_SKIPJACK_PRIVATE_WRAP:
	value = ((CK_SKIPJACK_PRIVATE_WRAP_PTR) mechanism->pParameter)->pPassword;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_PRIVATE_WRAP_PTR) mechanism->pParameter)->pPublicData;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_PRIVATE_WRAP_PTR) mechanism->pParameter)->pRandomA;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_PRIVATE_WRAP_PTR) mechanism->pParameter)->pPrimeP;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_PRIVATE_WRAP_PTR) mechanism->pParameter)->pBaseG;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_PRIVATE_WRAP_PTR) mechanism->pParameter)->pSubprimeQ;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_SKIPJACK_RELAYX:
	value = ((CK_SKIPJACK_RELAYX_PARAMS_PTR) mechanism->pParameter)->pOldWrappedX;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_RELAYX_PARAMS_PTR) mechanism->pParameter)->pOldPassword;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_RELAYX_PARAMS_PTR) mechanism->pParameter)->pOldPublicData;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_RELAYX_PARAMS_PTR) mechanism->pParameter)->pOldRandomA;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_RELAYX_PARAMS_PTR) mechanism->pParameter)->pNewPassword;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_RELAYX_PARAMS_PTR) mechanism->pParameter)->pNewPublicData;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SKIPJACK_RELAYX_PARAMS_PTR) mechanism->pParameter)->pNewRandomA;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_PBE_MD2_DES_CBC:
    case CKM_PBE_MD5_DES_CBC:
    case CKM_PBE_MD5_CAST_CBC:
    case CKM_PBE_MD5_CAST3_CBC:
    case CKM_PBE_MD5_CAST128_CBC:
	
	    /* case CKM_PBE_MD5_CAST5_CBC: */ 
    case CKM_PBE_SHA1_CAST128_CBC:
	
	    /* case CKM_PBE_SHA1_CAST5_CBC: */ 
    case CKM_PBE_SHA1_RC4_128:
    case CKM_PBE_SHA1_RC4_40:
    case CKM_PBE_SHA1_DES3_EDE_CBC:
    case CKM_PBE_SHA1_DES2_EDE_CBC:
    case CKM_PBE_SHA1_RC2_128_CBC:
    case CKM_PBE_SHA1_RC2_40_CBC:
    case CKM_PBA_SHA1_WITH_SHA1_HMAC:
	value = ((CK_PBE_PARAMS_PTR) mechanism->pParameter)->pInitVector;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_PBE_PARAMS_PTR) mechanism->pParameter)->pPassword;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_PBE_PARAMS_PTR) mechanism->pParameter)->pSalt;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_PKCS5_PBKD2:
	value = ((CK_PKCS5_PBKD2_PARAMS_PTR) mechanism->pParameter)->pSaltSourceData;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_PKCS5_PBKD2_PARAMS_PTR) mechanism->pParameter)->pPrfData;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_CONCATENATE_BASE_AND_DATA:
    case CKM_XOR_BASE_AND_DATA:
    case CKM_DES_ECB_ENCRYPT_DATA:
    case CKM_DES3_ECB_ENCRYPT_DATA:
    case CKM_AES_ECB_ENCRYPT_DATA:
	value = ((CK_KEY_DERIVATION_STRING_DATA_PTR) mechanism->pParameter)->pData;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_AES_GCM:
	value = ((CK_GCM_PARAMS_PTR) mechanism->pParameter)->pIv;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_GCM_PARAMS_PTR) mechanism->pParameter)->pAAD;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_AES_CCM:
	value = ((CK_CCM_PARAMS_PTR) mechanism->pParameter)->pNonce;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_CCM_PARAMS_PTR) mechanism->pParameter)->pAAD;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_KEY_WRAP_SET_OAEP:
	value = ((CK_KEY_WRAP_SET_OAEP_PARAMS_PTR) mechanism->pParameter)->pX;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_SSL3_MASTER_KEY_DERIVE:
    case CKM_SSL3_MASTER_KEY_DERIVE_DH:
    case CKM_TLS_MASTER_KEY_DERIVE:
    case CKM_TLS_MASTER_KEY_DERIVE_DH:
	value = ((CK_SSL3_MASTER_KEY_DERIVE_PARAMS_PTR) mechanism->pParameter)->RandomInfo.pClientRandom;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SSL3_MASTER_KEY_DERIVE_PARAMS_PTR) mechanism->pParameter)->RandomInfo.pServerRandom;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SSL3_MASTER_KEY_DERIVE_PARAMS_PTR) mechanism->pParameter)->pVersion;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_SSL3_KEY_AND_MAC_DERIVE:
    case CKM_TLS_KEY_AND_MAC_DERIVE:
	value = ((CK_SSL3_KEY_MAT_PARAMS_PTR) mechanism->pParameter)->RandomInfo.pClientRandom;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SSL3_KEY_MAT_PARAMS_PTR) mechanism->pParameter)->RandomInfo.pServerRandom;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SSL3_KEY_MAT_PARAMS_PTR) mechanism->pParameter)->pReturnedKeyMaterial->pIVClient;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_SSL3_KEY_MAT_PARAMS_PTR) mechanism->pParameter)->pReturnedKeyMaterial->pIVServer;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_ECDH1_DERIVE:
    case CKM_ECDH1_COFACTOR_DERIVE:
	value = ((CK_ECDH1_DERIVE_PARAMS_PTR) mechanism->pParameter)->pSharedData;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_ECDH1_DERIVE_PARAMS_PTR) mechanism->pParameter)->pPublicData;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_ECMQV_DERIVE:
	value = ((CK_ECDH2_DERIVE_PARAMS_PTR) mechanism->pParameter)->pSharedData;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_ECDH2_DERIVE_PARAMS_PTR) mechanism->pParameter)->pPublicData;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_ECDH2_DERIVE_PARAMS_PTR) mechanism->pParameter)->pPublicData2;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_X9_42_DH_DERIVE:
	value = ((CK_X9_42_DH1_DERIVE_PARAMS_PTR) mechanism->pParameter)->pOtherInfo;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_X9_42_DH1_DERIVE_PARAMS_PTR) mechanism->pParameter)->pPublicData;
	if (value != NULL_PTR)
	    free(value);
	break;
    case CKM_X9_42_DH_HYBRID_DERIVE:
    case CKM_X9_42_MQV_DERIVE:
	value = ((CK_X9_42_DH2_DERIVE_PARAMS_PTR) mechanism->pParameter)->pOtherInfo;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_X9_42_DH2_DERIVE_PARAMS_PTR) mechanism->pParameter)->pPublicData;
	if (value != NULL_PTR)
	    free(value);
	value = ((CK_X9_42_DH2_DERIVE_PARAMS_PTR) mechanism->pParameter)->pPublicData2;
	if (value != NULL_PTR)
	    free(value);
	break;
    }
     
	/* free parameter structure itself */ 
	free(mechanism->pParameter);
}

 
/* ************************************************************************** */ 
/* The native implementation of the methods of the PKCS11Implementation class */ 
/* for random generation and some general and legacy functions                                           */ 
/* ************************************************************************** */ 
    
/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    connect
 * Signature: (Ljava/lang/String;)V
 */ 
/* see platform.c, because the implementation is platform dependent */ 
     
/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    disconnect
 * Signature: ()V
 */ 
/* see platform.c, because the implementation is platform dependent */ 
     
/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_Initialize
 * Signature: (Ljava/lang/Object;Z)V
 * Parametermapping:                    *PKCS11*
 * @param   jobject jInitArgs           CK_VOID_PTR pInitArgs
 */ 
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1Initialize 
    (JNIEnv * env, jobject obj, jobject jInitArgs, jboolean jUseUtf8)  { 
	/*
	 * Initialize Cryptoki
	 */ 
	CK_C_INITIALIZE_ARGS_PTR ckpInitArgs;
    CK_RV rv;
    ModuleData * moduleData;
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
     if (jInitArgs != NULL_PTR) {
	ckpInitArgs = makeCKInitArgsAdapter(env, jInitArgs, jUseUtf8);
	if (ckpInitArgs == NULL_PTR) {
	    return;
	}
    } else {
	ckpInitArgs = NULL_PTR;
    }
     rv = (*ckpFunctions->C_Initialize) (ckpInitArgs);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);
     if (ckpInitArgs != NULL_PTR) {
	if (ckpInitArgs->pReserved != NULL_PTR) {
	    free(ckpInitArgs->pReserved);
	}
	free(ckpInitArgs);
    }
     TRACE0(tag_call, __FUNCTION__, "exiting ");
}

 
/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_Finalize
 * Signature: (Ljava/lang/Object;)V
 * Parametermapping:                    *PKCS11*
 * @param   jobject jReserved           CK_VOID_PTR pReserved
 */ 
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1Finalize 
    (JNIEnv * env, jobject obj, jobject jReserved)  { 
	/*
	 * Finalize Cryptoki
	 */ 
	CK_VOID_PTR ckpReserved;
    CK_RV rv;
    ModuleData * moduleData;
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
     ckpReserved = jObjectToCKVoidPtr(jReserved);
     rv = (*ckpFunctions->C_Finalize) (ckpReserved);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);
     TRACE0(tag_call, __FUNCTION__, "exiting ");
}

 
/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetInfo
 * Signature: ()Liaik/pkcs/pkcs11/wrapper/CK_INFO;
 * Parametermapping:                    *PKCS11*
 * @return  jobject jInfoObject         CK_INFO_PTR pInfo
 */ 
    JNIEXPORT jobject JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetInfo 
    (JNIEnv * env, jobject obj)  { CK_INFO ckLibInfo;
    jobject jInfoObject;
    CK_RV rv;
    ModuleData * moduleData;
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
     rv = (*ckpFunctions->C_GetInfo) (&ckLibInfo);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }
     jInfoObject = ckInfoPtrToJInfo(env, &ckLibInfo);
     TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jInfoObject;
}

 
/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_SeedRandom
 * Signature: (J[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jSeed            CK_BYTE_PTR pSeed
 *                                      CK_ULONG ulSeedLen
 */ 
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1SeedRandom 
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jSeed)  { CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpSeed = NULL_PTR;
    CK_ULONG ckSeedLength;
    CK_RV rv;
    ModuleData * moduleData;
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
    if (jByteArrayToCKByteArray(env, jSeed, &ckpSeed, &ckSeedLength)) {
	return;
    }
     rv = (*ckpFunctions->C_SeedRandom) (ckSessionHandle, ckpSeed, ckSeedLength);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);
     free(ckpSeed);
     TRACE0(tag_call, __FUNCTION__, "exiting ");
}

 
/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GenerateRandom
 * Signature: (J[B)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jbyteArray jRandomData      CK_BYTE_PTR pRandomData
 *                                      CK_ULONG ulRandomDataLen
 */ 
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GenerateRandom 
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jRandomData)  { CK_SESSION_HANDLE ckSessionHandle;
    jbyte * jRandomBuffer;
    jlong jRandomBufferLength;
    CK_RV rv;
    ModuleData * moduleData;
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
     jRandomBufferLength = (*env)->GetArrayLength(env, jRandomData);
    jRandomBuffer = (*env)->GetByteArrayElements(env, jRandomData, NULL_PTR);
     rv = (*ckpFunctions->C_GenerateRandom) (ckSessionHandle, 
					       (CK_BYTE_PTR) jRandomBuffer, jLongToCKULong(jRandomBufferLength));
    ckAssertReturnValueOK(env, rv, __FUNCTION__);
     
	/* copy back generated bytes */ 
	(*env)->ReleaseByteArrayElements(env, jRandomData, jRandomBuffer, 0);
     TRACE0(tag_call, __FUNCTION__, "exiting ");
}

 
/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetFunctionStatus
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 */ 
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetFunctionStatus 
    (JNIEnv * env, jobject obj, jlong jSessionHandle)  { CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    ModuleData * moduleData;
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
     
	/* C_GetFunctionStatus should always return CKR_FUNCTION_NOT_PARALLEL */ 
	rv = (*ckpFunctions->C_GetFunctionStatus) (ckSessionHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);
     TRACE0(tag_call, __FUNCTION__, "exiting ");
}

 
/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_CancelFunction
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 */ 
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1CancelFunction 
    (JNIEnv * env, jobject obj, jlong jSessionHandle)  { CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    ModuleData * moduleData;
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
     
	/* C_GetFunctionStatus should always return CKR_FUNCTION_NOT_PARALLEL */ 
	rv = (*ckpFunctions->C_CancelFunction) (ckSessionHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);
     TRACE0(tag_call, __FUNCTION__, "exiting ");
}

   
