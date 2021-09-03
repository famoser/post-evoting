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
    CK_FUNCTION_LIST_PTR getFunctionList(JNIEnv * env, ModuleData * moduleData) 
    
    
	throwPKCS11RuntimeException(env, (*env)->NewStringUTF(env, "This modules does not provide methods"));
	return NULL_PTR;
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
    
    
    
    
    
	
	
	
	
	
	    /* We must call the equals method as implemented by the Object class. This
	     * method compares if both references refer to the same object. This is what
	     * we want.
	     */ 
	    jequal = (*env)->CallNonvirtualBooleanMethod(env, thisObject, jObjectClass, jequals, otherObject);
    
	
    
    
    



{
    
    
	/* free pointers inside parameter structures, see jMechanismParameterToCKMechanismParameter */ 
	switch (mechanism->mechanism) {
    
	
	
	    free(value);
	
    
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
    
    
	
	
	    free(value);
	
    
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
    
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
    
    
    
    
    
	
	    /* case CKM_PBE_MD5_CAST5_CBC: */ 
    case CKM_PBE_SHA1_CAST128_CBC:
	
	    /* case CKM_PBE_SHA1_CAST5_CBC: */ 
    case CKM_PBE_SHA1_RC4_128:
    
    
    
    
    
    
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
    
	
	
	    free(value);
	
	
	    free(value);
	
    
    
    
    
    
	
	
	    free(value);
	
    
	
	
	    free(value);
	
	
	    free(value);
	
    
	
	
	    free(value);
	
	
	    free(value);
	
    
	
	
	    free(value);
	
    
    
    
    
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
    
    
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
    
    
	
	
	    free(value);
	
	
	    free(value);
	
    
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
    
	
	
	    free(value);
	
	
	    free(value);
	
    
    
	
	
	    free(value);
	
	
	    free(value);
	
	
	    free(value);
	
    
    
	/* free parameter structure itself */ 
	free(mechanism->pParameter);



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
    (JNIEnv * env, jobject obj, jobject jInitArgs, jboolean jUseUtf8) 
	/*
	 * Initialize Cryptoki
	 */ 
	CK_C_INITIALIZE_ARGS_PTR ckpInitArgs;
    
    
    
    
    
    
	throwDisconnectedRuntimeException(env);
	return;
    }
    
    
	return;
    }
    
	
	
	    return;
	}
    
	
    
    
    
    
	
	    
	
	
    
    



/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_Finalize
 * Signature: (Ljava/lang/Object;)V
 * Parametermapping:                    *PKCS11*
 * @param   jobject jReserved           CK_VOID_PTR pReserved
 */ 
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1Finalize 
    (JNIEnv * env, jobject obj, jobject jReserved) 
	/*
	 * Finalize Cryptoki
	 */ 
	CK_VOID_PTR ckpReserved;
    
    
    
    
    
    
	throwDisconnectedRuntimeException(env);
	return;
    }
    
    
	return;
    }
    
    
    
    



/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetInfo
 * Signature: ()Liaik/pkcs/pkcs11/wrapper/CK_INFO;
 * Parametermapping:                    *PKCS11*
 * @return  jobject jInfoObject         CK_INFO_PTR pInfo
 */ 
    JNIEXPORT jobject JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetInfo 
    (JNIEnv * env, jobject obj) 
    
    
    
    
    
    
    
	throwDisconnectedRuntimeException(env);
	return NULL_PTR;
    }
    
    
	return NULL_PTR;
    }
    
    
	return NULL_PTR;
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
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jSeed) 
    
    
    
    
    
    
    
    
	throwDisconnectedRuntimeException(env);
	return;
    }
    
    
	return;
    }
    
    
	return;
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
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jRandomData) 
    
    
    
    
    
    
    
    
	throwDisconnectedRuntimeException(env);
	return;
    }
    
    
	return;
    }
    
    
    
    
					       (CK_BYTE_PTR) jRandomBuffer, 
    
    
	/* copy back generated bytes */ 
	(*env)->ReleaseByteArrayElements(env, jRandomData, jRandomBuffer, 0);
    



/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetFunctionStatus
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 */ 
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetFunctionStatus 
    (JNIEnv * env, jobject obj, jlong jSessionHandle) 
    
    
    
    
    
    
	throwDisconnectedRuntimeException(env);
	return;
    }
    
    
	return;
    }
    
    
	/* C_GetFunctionStatus should always return CKR_FUNCTION_NOT_PARALLEL */ 
	rv = (*ckpFunctions->C_GetFunctionStatus) (ckSessionHandle);
    
    



/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_CancelFunction
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 */ 
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1CancelFunction 
    (JNIEnv * env, jobject obj, jlong jSessionHandle) 
    
    
    
    
    
    
	throwDisconnectedRuntimeException(env);
	return;
    }
    
    
	return;
    }
    
    
	/* C_GetFunctionStatus should always return CKR_FUNCTION_NOT_PARALLEL */ 
	rv = (*ckpFunctions->C_CancelFunction) (ckSessionHandle);
    
    


