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
/* Mechanism parameter conversion functions between Java and Cryptoki types   */
/* ************************************************************************** */

/*
 * converts the Java CK_DES_CBC_ENCRYPT_DATA_PARAMS object to a CK_DES_CBC_ENCRYPT_DATA_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_DES_CBC_ENCRYPT_DATA_PARAMS object to convert
 * @return - the new CK_DES_CBC_ENCRYPT_DATA_PARAMS structure
 */
CK_DES_CBC_ENCRYPT_DATA_PARAMS jDesCbcEncryptDataParamToCKDesCbcEncryptData(JNIEnv * env, jobject jParam)
{
    jclass jDesCbcEncryptDataParamsClass = (*env)->FindClass(env, CLASS_DES_CBC_ENCRYPT_DATA_PARAMS);
    CK_DES_CBC_ENCRYPT_DATA_PARAMS ckParam;
    jfieldID fieldID;
    jobject jObject;
    CK_BYTE_PTR ckpByte;
    CK_ULONG ivLength;

    /* get iv */
    fieldID = (*env)->GetFieldID(env, jDesCbcEncryptDataParamsClass, "iv", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &ckpByte, &ivLength);
    memcpy(ckParam.iv, ckpByte, ivLength);
    free(ckpByte);

    /* get pData and length */
    fieldID = (*env)->GetFieldID(env, jDesCbcEncryptDataParamsClass, "pData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &ckpByte, &(ckParam.length));
    ckParam.pData = (CK_VOID_PTR) ckpByte;

    return ckParam;
}

/*
 * converts the Java CK_AES_CBC_ENCRYPT_DATA_PARAMS object to a CK_AES_CBC_ENCRYPT_DATA_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_AES_CBC_ENCRYPT_DATA_PARAMS object to convert
 * @return - the new CK_AES_CBC_ENCRYPT_DATA_PARAMS structure
 */
CK_AES_CBC_ENCRYPT_DATA_PARAMS jAesCbcEncryptDataParamToCKAesCbcEncryptData(JNIEnv * env, jobject jParam)
{
    jclass jAesCbcEncryptDataParamsClass = (*env)->FindClass(env, CLASS_AES_CBC_ENCRYPT_DATA_PARAMS);
    CK_AES_CBC_ENCRYPT_DATA_PARAMS ckParam;
    jfieldID fieldID;
    jobject jObject;
    CK_BYTE_PTR ckpByte;
    CK_ULONG ivLength;

    /* get iv */
    fieldID = (*env)->GetFieldID(env, jAesCbcEncryptDataParamsClass, "iv", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &ckpByte, &ivLength);
    memcpy(ckParam.iv, ckpByte, ivLength);
    free(ckpByte);

    /* get pData and length */
    fieldID = (*env)->GetFieldID(env, jAesCbcEncryptDataParamsClass, "pData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &ckpByte, &(ckParam.length));
    ckParam.pData = (CK_VOID_PTR) ckpByte;

    return ckParam;
}

/*
 * converts the Java CK_GCM_PARAMS object to a CK_GCM_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_GCM_PARAMS object to convert
 * @return - the new CK_GCM_PARAMS structure
 */
CK_GCM_PARAMS jGcmParamToCKGcmData(JNIEnv * env, jobject jParam)
{
    jclass jGcmParamsClass = (*env)->FindClass(env, CLASS_GCM_PARAMS);
    CK_GCM_PARAMS ckParam;
    jfieldID fieldID;
    jobject jObject;
    jlong jLong;
    CK_BYTE_PTR ckpiv;
    CK_BYTE_PTR ckpaad;
    CK_ULONG ivLength;
    CK_ULONG aadLength;

    fieldID = (*env)->GetFieldID(env, jGcmParamsClass, "pIv", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &ckpiv, &ivLength);
    ckParam.pIv = (CK_BYTE_PTR) ckpiv;
    ckParam.ulIvLen = ivLength;
    ckParam.ulIvBits = ivLength * 8;

    /* get aad */
    fieldID = (*env)->GetFieldID(env, jGcmParamsClass, "pAAD", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &ckpaad, &aadLength);
    ckParam.pAAD = ckpaad;
    ckParam.ulAADLen = aadLength;

    /* get ulTagBits */
    fieldID = (*env)->GetFieldID(env, jGcmParamsClass, "ulTagBits", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulTagBits = jLongToCKULong(jLong);

    return ckParam;
}

/*
 * converts the Java CK_CCM_PARAMS object to a CK_CCM_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_CCM_PARAMS object to convert
 * @return - the new CK_CCM_PARAMS structure
 */
CK_CCM_PARAMS jCcmParamToCKCcmData(JNIEnv * env, jobject jParam)
{
    jclass jCcmParamsClass = (*env)->FindClass(env, CLASS_CCM_PARAMS);
    CK_CCM_PARAMS ckParam;
    jfieldID fieldID;
    jobject jObject;
    jlong jLong;
    CK_BYTE_PTR ckpnonce;
    CK_BYTE_PTR ckpaad;
    CK_ULONG nonceLength;
    CK_ULONG aadLength;

    /* get nonce */
    fieldID = (*env)->GetFieldID(env, jCcmParamsClass, "pNonce", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &ckpnonce, &nonceLength);
    ckParam.pNonce = (CK_BYTE_PTR) ckpnonce;
    ckParam.ulNonceLen = nonceLength;

    /* get aad */
    fieldID = (*env)->GetFieldID(env, jCcmParamsClass, "pAAD", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &ckpaad, &aadLength);
    ckParam.pAAD = ckpaad;
    ckParam.ulAADLen = aadLength;

    /* get DataLen */
    fieldID = (*env)->GetFieldID(env, jCcmParamsClass, "ulDataLen", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulDataLen = jLongToCKULong(jLong);

    /* get MacLen */
    fieldID = (*env)->GetFieldID(env, jCcmParamsClass, "ulMacLen", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulMACLen = jLongToCKULong(jLong);

    return ckParam;
}

/*
 * converts the Java CK_RSA_PKCS_OAEP_PARAMS object to a CK_RSA_PKCS_OAEP_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_RSA_PKCS_OAEP_PARAMS object to convert
 * @return - the new CK_RSA_PKCS_OAEP_PARAMS structure
 */
CK_RSA_PKCS_OAEP_PARAMS jRsaPkcsOaepParamToCKRsaPkcsOaepParam(JNIEnv * env, jobject jParam)
{
    jclass jRsaPkcsOaepParamsClass = (*env)->FindClass(env, CLASS_RSA_PKCS_OAEP_PARAMS);
    CK_RSA_PKCS_OAEP_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;
    jobject jObject;
    CK_BYTE_PTR ckpByte;

    /* get hashAlg */
    fieldID = (*env)->GetFieldID(env, jRsaPkcsOaepParamsClass, "hashAlg", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.hashAlg = jLongToCKULong(jLong);

    /* get mgf */
    fieldID = (*env)->GetFieldID(env, jRsaPkcsOaepParamsClass, "mgf", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.mgf = jLongToCKULong(jLong);

    /* get source */
    fieldID = (*env)->GetFieldID(env, jRsaPkcsOaepParamsClass, "source", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.source = jLongToCKULong(jLong);

    /* get sourceData and sourceDataLength */
    fieldID = (*env)->GetFieldID(env, jRsaPkcsOaepParamsClass, "pSourceData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &ckpByte, &(ckParam.ulSourceDataLen));
    ckParam.pSourceData = (CK_VOID_PTR) ckpByte;

    return ckParam;
}

/*
 * converts the Java CK_KEA_DERIVE_PARAMS object to a CK_KEA_DERIVE_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_KEA_DERIVE_PARAMS object to convert
 * @return - the new CK_KEA_DERIVE_PARAMS structure
 */
CK_KEA_DERIVE_PARAMS jKeaDeriveParamToCKKeaDeriveParam(JNIEnv * env, jobject jParam)
{
    jclass jKeaDeriveParamsClass = (*env)->FindClass(env, CLASS_KEA_DERIVE_PARAMS);
    CK_KEA_DERIVE_PARAMS ckParam;
    jfieldID fieldID;
    jboolean jBoolean;
    jobject jObject;
    CK_ULONG ckTemp;

    /* get isSender */
    fieldID = (*env)->GetFieldID(env, jKeaDeriveParamsClass, "isSender", "Z");
    assert(fieldID != 0);
    jBoolean = (*env)->GetBooleanField(env, jParam, fieldID);
    ckParam.isSender = jBooleanToCKBBool(jBoolean);

    /* get pRandomA and ulRandomLength */
    fieldID = (*env)->GetFieldID(env, jKeaDeriveParamsClass, "pRandomA", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pRandomA), &ckTemp);

    /* get pRandomB and ulRandomLength */
    fieldID = (*env)->GetFieldID(env, jKeaDeriveParamsClass, "pRandomB", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pRandomB), &(ckParam.ulRandomLen));
    /* pRandomA and pRandomB must have the same length */
    assert(ckTemp == ckParam.ulRandomLen);	/* pRandomALength == pRandomBLength */

    /* get pPublicData and ulPublicDataLength */
    fieldID = (*env)->GetFieldID(env, jKeaDeriveParamsClass, "pPublicData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pPublicData), &(ckParam.ulPublicDataLen));

    return ckParam;
}

/*
 * converts the Java CK_RC2_CBC_PARAMS object to a CK_RC2_CBC_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_RC2_CBC_PARAMS object to convert
 * @return - the new CK_RC2_CBC_PARAMS structure
 */
CK_RC2_CBC_PARAMS jRc2CbcParamToCKRc2CbcParam(JNIEnv * env, jobject jParam)
{
    jclass jRc2CbcParamsClass = (*env)->FindClass(env, CLASS_RC2_CBC_PARAMS);
    CK_RC2_CBC_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;
    jbyte *jpTemp;
    CK_ULONG i;
    jbyteArray jArray;
    jint jLength;
    CK_ULONG ckLength;

    /* get ulEffectiveBits */
    fieldID = (*env)->GetFieldID(env, jRc2CbcParamsClass, "ulEffectiveBits", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulEffectiveBits = jLongToCKULong(jLong);

    /* get iv[8] */
    fieldID = (*env)->GetFieldID(env, jRc2CbcParamsClass, "iv", "[B");
    assert(fieldID != 0);
    jArray = (jbyteArray) (*env)->GetObjectField(env, jParam, fieldID);
    assert(jArray != NULL_PTR);

    jLength = (*env)->GetArrayLength(env, jArray);
    assert(jLength == 8);	/*  iv is a BYTE[8] array */
    ckLength = jIntToCKULong(jLength);
    jpTemp = (jbyte *) malloc(ckLength * sizeof(jbyte));
    if (jpTemp == NULL_PTR && ckLength != 0) {
	throwOutOfMemoryError(env);
	return ckParam;
    }
    (*env)->GetByteArrayRegion(env, jArray, 0, ckLength, jpTemp);
    for (i = 0; i < ckLength; i++) {
	(ckParam.iv)[i] = jByteToCKByte(jpTemp[i]);
    }
    free(jpTemp);

    return ckParam;
}

/*
 * converts the Java CK_RC2_MAC_GENERAL_PARAMS object to a CK_RC2_MAC_GENERAL_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_RC2_MAC_GENERAL_PARAMS object to convert
 * @return - the new CK_RC2_MAC_GENERAL_PARAMS structure
 */
CK_RC2_MAC_GENERAL_PARAMS jRc2MacGeneralParamToCKRc2MacGeneralParam(JNIEnv * env, jobject jParam)
{
    jclass jRc2MacGeneralParamsClass = (*env)->FindClass(env, CLASS_RC2_MAC_GENERAL_PARAMS);
    CK_RC2_MAC_GENERAL_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;

    /* get ulEffectiveBits */
    fieldID = (*env)->GetFieldID(env, jRc2MacGeneralParamsClass, "ulEffectiveBits", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulEffectiveBits = jLongToCKULong(jLong);

    /* get ulMacLength */
    fieldID = (*env)->GetFieldID(env, jRc2MacGeneralParamsClass, "ulMacLength", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulMacLength = jLongToCKULong(jLong);

    return ckParam;
}

/*
 * converts the Java CK_RC5_PARAMS object to a CK_RC5_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_RC5_PARAMS object to convert
 * @return - the new CK_RC5_PARAMS structure
 */
CK_RC5_PARAMS jRc5ParamToCKRc5Param(JNIEnv * env, jobject jParam)
{
    jclass jRc5ParamsClass = (*env)->FindClass(env, CLASS_RC5_PARAMS);
    CK_RC5_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;

    /* get ulWordsize */
    fieldID = (*env)->GetFieldID(env, jRc5ParamsClass, "ulWordsize", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulWordsize = jLongToCKULong(jLong);

    /* get ulRounds */
    fieldID = (*env)->GetFieldID(env, jRc5ParamsClass, "ulRounds", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulRounds = jLongToCKULong(jLong);

    return ckParam;
}

/*
 * converts the Java CK_RC5_CBC_PARAMS object to a CK_RC5_CBC_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_RC5_CBC_PARAMS object to convert
 * @return - the new CK_RC5_CBC_PARAMS structure
 */
CK_RC5_CBC_PARAMS jRc5CbcParamToCKRc5CbcParam(JNIEnv * env, jobject jParam)
{
    jclass jRc5CbcParamsClass = (*env)->FindClass(env, CLASS_RC5_CBC_PARAMS);
    CK_RC5_CBC_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;
    jobject jObject;

    /* get ulWordsize */
    fieldID = (*env)->GetFieldID(env, jRc5CbcParamsClass, "ulWordsize", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulWordsize = jLongToCKULong(jLong);

    /* get ulRounds */
    fieldID = (*env)->GetFieldID(env, jRc5CbcParamsClass, "ulRounds", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulRounds = jLongToCKULong(jLong);

    /* get pIv and ulIvLen */
    fieldID = (*env)->GetFieldID(env, jRc5CbcParamsClass, "pIv", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pIv), &(ckParam.ulIvLen));

    return ckParam;
}

/*
 * converts the Java CK_RC5_MAC_GENERAL_PARAMS object to a CK_RC5_MAC_GENERAL_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_RC5_MAC_GENERAL_PARAMS object to convert
 * @return - the new CK_RC5_MAC_GENERAL_PARAMS structure
 */
CK_RC5_MAC_GENERAL_PARAMS jRc5MacGeneralParamToCKRc5MacGeneralParam(JNIEnv * env, jobject jParam)
{
    jclass jRc5MacGeneralParamsClass = (*env)->FindClass(env, CLASS_RC5_MAC_GENERAL_PARAMS);
    CK_RC5_MAC_GENERAL_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;

    /* get ulWordsize */
    fieldID = (*env)->GetFieldID(env, jRc5MacGeneralParamsClass, "ulWordsize", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulWordsize = jLongToCKULong(jLong);

    /* get ulRounds */
    fieldID = (*env)->GetFieldID(env, jRc5MacGeneralParamsClass, "ulRounds", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulRounds = jLongToCKULong(jLong);

    /* get ulMacLength */
    fieldID = (*env)->GetFieldID(env, jRc5MacGeneralParamsClass, "ulMacLength", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulMacLength = jLongToCKULong(jLong);

    return ckParam;
}

/*
 * converts the Java CK_SKIPJACK_PRIVATE_WRAP_PARAMS object to a CK_SKIPJACK_PRIVATE_WRAP_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_SKIPJACK_PRIVATE_WRAP_PARAMS object to convert
 * @return - the new CK_SKIPJACK_PRIVATE_WRAP_PARAMS structure
 */
CK_SKIPJACK_PRIVATE_WRAP_PARAMS jSkipjackPrivateWrapParamToCKSkipjackPrivateWrapParam(JNIEnv * env, jobject jParam)
{
    jclass jSkipjackPrivateWrapParamsClass = (*env)->FindClass(env, CLASS_SKIPJACK_PRIVATE_WRAP_PARAMS);
    CK_SKIPJACK_PRIVATE_WRAP_PARAMS ckParam;
    jfieldID fieldID;
    jobject jObject;
    CK_ULONG ckTemp;

    /* get pPassword and ulPasswordLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackPrivateWrapParamsClass, "pPassword", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pPassword), &(ckParam.ulPasswordLen));

    /* get pPublicData and ulPublicDataLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackPrivateWrapParamsClass, "pPublicData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pPublicData), &(ckParam.ulPublicDataLen));

    /* get pRandomA and ulRandomLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackPrivateWrapParamsClass, "pRandomA", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pRandomA), &(ckParam.ulRandomLen));

    /* get pPrimeP and ulPandGLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackPrivateWrapParamsClass, "pPrimeP", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pPrimeP), &ckTemp);

    /* get pBaseG and ulPAndGLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackPrivateWrapParamsClass, "pBaseG", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pBaseG), &(ckParam.ulPAndGLen));
    /* pPrimeP and pBaseG must have the same length */
    assert(ckTemp == ckParam.ulPAndGLen);

    /* get pSubprimeQ and ulQLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackPrivateWrapParamsClass, "pSubprimeQ", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pSubprimeQ), &(ckParam.ulQLen));

    return ckParam;
}

/*
 * converts the Java CK_SKIPJACK_RELAYX_PARAMS object to a CK_SKIPJACK_RELAYX_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_SKIPJACK_RELAYX_PARAMS object to convert
 * @return - the new CK_SKIPJACK_RELAYX_PARAMS structure
 */
CK_SKIPJACK_RELAYX_PARAMS jSkipjackRelayxParamToCKSkipjackRelayxParam(JNIEnv * env, jobject jParam)
{
    jclass jSkipjackRelayxParamsClass = (*env)->FindClass(env, CLASS_SKIPJACK_RELAYX_PARAMS);
    CK_SKIPJACK_RELAYX_PARAMS ckParam;
    jfieldID fieldID;
    jobject jObject;

    /* get pOldWrappedX and ulOldWrappedXLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackRelayxParamsClass, "pOldWrappedX", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pOldWrappedX), &(ckParam.ulOldWrappedXLen));

    /* get pOldPassword and ulOldPasswordLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackRelayxParamsClass, "pOldPassword", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pOldPassword), &(ckParam.ulOldPasswordLen));

    /* get pOldPublicData and ulOldPublicDataLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackRelayxParamsClass, "pOldPublicData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pOldPublicData), &(ckParam.ulOldPublicDataLen));

    /* get pOldRandomA and ulOldRandomLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackRelayxParamsClass, "pOldRandomA", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pOldRandomA), &(ckParam.ulOldRandomLen));

    /* get pNewPassword and ulNewPasswordLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackRelayxParamsClass, "pNewPassword", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pNewPassword), &(ckParam.ulNewPasswordLen));

    /* get pNewPublicData and ulNewPublicDataLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackRelayxParamsClass, "pNewPublicData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pNewPublicData), &(ckParam.ulNewPublicDataLen));

    /* get pNewRandomA and ulNewRandomLength */
    fieldID = (*env)->GetFieldID(env, jSkipjackRelayxParamsClass, "pNewRandomA", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pNewRandomA), &(ckParam.ulNewRandomLen));

    return ckParam;
}

/*
 * converts the Java CK_PBE_PARAMS object to a CK_PBE_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_PBE_PARAMS object to convert
 * @return - the new CK_PBE_PARAMS structure
 */
CK_PBE_PARAMS jPbeParamToCKPbeParam(JNIEnv * env, jobject jParam)
{
    jclass jPbeParamsClass = (*env)->FindClass(env, CLASS_PBE_PARAMS);
    CK_PBE_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;
    jobject jObject;
    CK_ULONG ckTemp;

    /* get pInitVector */
    fieldID = (*env)->GetFieldID(env, jPbeParamsClass, "pInitVector", "[C");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jCharArrayToCKCharArray(env, jObject, &(ckParam.pInitVector), &ckTemp);

    /* get pPassword and ulPasswordLength */
    fieldID = (*env)->GetFieldID(env, jPbeParamsClass, "pPassword", "[C");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jCharArrayToCKCharArray(env, jObject, &(ckParam.pPassword), &(ckParam.ulPasswordLen));

    /* get pSalt and ulSaltLength */
    fieldID = (*env)->GetFieldID(env, jPbeParamsClass, "pSalt", "[C");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jCharArrayToCKCharArray(env, jObject, &(ckParam.pSalt), &(ckParam.ulSaltLen));

    /* get ulIteration */
    fieldID = (*env)->GetFieldID(env, jPbeParamsClass, "ulIteration", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulIteration = jLongToCKULong(jLong);

    return ckParam;
}

/*
 * Copy back the initialization vector from the native structure to the
 * Java object. This is only used for CKM_PBE_* mechanisms and their
 * CK_PBE_PARAMS parameters.
 *
 */
void copyBackPBEInitializationVector(JNIEnv * env, CK_MECHANISM * ckMechanism, jobject jMechanism)
{
    jclass jMechanismClass = (*env)->FindClass(env, CLASS_MECHANISM);
    jclass jPbeParamsClass = (*env)->FindClass(env, CLASS_PBE_PARAMS);
    CK_PBE_PARAMS *ckParam;
    jfieldID fieldID;
    CK_MECHANISM_TYPE ckMechanismType;
    jlong jMechanismType;
    jobject jParameter;
    jobject jInitVector;
    jint jInitVectorLength;
    CK_CHAR_PTR initVector;
    int i;
    jchar *jInitVectorChars;

    /* get mechanism */
    fieldID = (*env)->GetFieldID(env, jMechanismClass, "mechanism", "J");
    assert(fieldID != 0);
    jMechanismType = (*env)->GetLongField(env, jMechanism, fieldID);
    ckMechanismType = jLongToCKULong(jMechanismType);
    if (ckMechanismType != ckMechanism->mechanism) {
	/* we do not have matching types, this should not occur */
	return;
    }

    ckParam = (CK_PBE_PARAMS *) ckMechanism->pParameter;
    if (ckParam != NULL_PTR) {
	initVector = ckParam->pInitVector;
	if (initVector != NULL_PTR) {
	    /* get pParameter */
	    fieldID = (*env)->GetFieldID(env, jMechanismClass, "pParameter", "Ljava/lang/Object;");
	    assert(fieldID != 0);
	    jParameter = (*env)->GetObjectField(env, jMechanism, fieldID);
	    fieldID = (*env)->GetFieldID(env, jPbeParamsClass, "pInitVektor", "[C");
	    assert(fieldID != 0);
	    jInitVector = (*env)->GetObjectField(env, jParameter, fieldID);

	    if (jInitVector != NULL_PTR) {
		jInitVectorLength = (*env)->GetArrayLength(env, jInitVector);
		jInitVectorChars = (*env)->GetCharArrayElements(env, jInitVector, NULL_PTR);
		/* copy the chars to the Java buffer */
		for (i = 0; i < jInitVectorLength; i++) {
		    jInitVectorChars[i] = ckCharToJChar(initVector[i]);
		}
		/* copy back the Java buffer to the object */
		(*env)->ReleaseCharArrayElements(env, jInitVector, jInitVectorChars, 0);
	    }
	}
    }
}

/*
 * converts the Java CK_PKCS5_PBKD2_PARAMS object to a CK_PKCS5_PBKD2_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_PKCS5_PBKD2_PARAMS object to convert
 * @return - the new CK_PKCS5_PBKD2_PARAMS structure
 */
CK_PKCS5_PBKD2_PARAMS jPkcs5Pbkd2ParamToCKPkcs5Pbkd2Param(JNIEnv * env, jobject jParam)
{
    jclass jPkcs5Pbkd2ParamsClass = (*env)->FindClass(env, CLASS_PKCS5_PBKD2_PARAMS);
    CK_PKCS5_PBKD2_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;
    jobject jObject;

    /* get saltSource */
    fieldID = (*env)->GetFieldID(env, jPkcs5Pbkd2ParamsClass, "saltSource", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.saltSource = jLongToCKULong(jLong);

    /* get pSaltSourceData */
    fieldID = (*env)->GetFieldID(env, jPkcs5Pbkd2ParamsClass, "pSaltSourceData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, (CK_BYTE_PTR *) & (ckParam.pSaltSourceData), &(ckParam.ulSaltSourceDataLen));

    /* get iterations */
    fieldID = (*env)->GetFieldID(env, jPkcs5Pbkd2ParamsClass, "iterations", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.iterations = jLongToCKULong(jLong);

    /* get prf */
    fieldID = (*env)->GetFieldID(env, jPkcs5Pbkd2ParamsClass, "prf", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.prf = jLongToCKULong(jLong);

    /* get pPrfData and ulPrfDataLength in byte */
    fieldID = (*env)->GetFieldID(env, jPkcs5Pbkd2ParamsClass, "pPrfData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, (CK_BYTE_PTR *) & (ckParam.pPrfData), &(ckParam.ulPrfDataLen));

    return ckParam;
}

/*
 * converts the Java CK_KEY_WRAP_SET_OAEP_PARAMS object to a CK_KEY_WRAP_SET_OAEP_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_KEY_WRAP_SET_OAEP_PARAMS object to convert
 * @return - the new CK_KEY_WRAP_SET_OAEP_PARAMS structure
 */
CK_KEY_WRAP_SET_OAEP_PARAMS jKeyWrapSetOaepParamToCKKeyWrapSetOaepParam(JNIEnv * env, jobject jParam)
{
    jclass jKeyWrapSetOaepParamsClass = (*env)->FindClass(env, CLASS_KEY_WRAP_SET_OAEP_PARAMS);
    CK_KEY_WRAP_SET_OAEP_PARAMS ckParam;
    jfieldID fieldID;
    jbyte jByte;
    jobject jObject;

    /* get bBC */
    fieldID = (*env)->GetFieldID(env, jKeyWrapSetOaepParamsClass, "bBC", "B");
    assert(fieldID != 0);
    jByte = (*env)->GetByteField(env, jParam, fieldID);
    ckParam.bBC = jByteToCKByte(jByte);

    /* get pX and ulXLength */
    fieldID = (*env)->GetFieldID(env, jKeyWrapSetOaepParamsClass, "pX", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pX), &(ckParam.ulXLen));

    return ckParam;
}

/*
 * Copy back the unwrapped key info from the native structure to the
 * Java object. This is only used for the CK_KEY_WRAP_SET_OAEP_PARAMS
 * mechanism when used for unwrapping a key.
 *
 */
void copyBackSetUnwrappedKey(JNIEnv * env, CK_MECHANISM * ckMechanism, jobject jMechanism)
{
    jclass jMechanismClass = (*env)->FindClass(env, CLASS_MECHANISM);
    jclass jSetParamsClass = (*env)->FindClass(env, CLASS_KEY_WRAP_SET_OAEP_PARAMS);
    CK_KEY_WRAP_SET_OAEP_PARAMS *ckKeyWrapSetOaepParams;
    jfieldID fieldID;
    CK_MECHANISM_TYPE ckMechanismType;
    jlong jMechanismType;
    CK_BYTE_PTR x;
    jobject jParameter;
    jobject jx;
    jint jxLength;
    jbyte *jxBytes;
    int i;

    /* get mechanism */
    fieldID = (*env)->GetFieldID(env, jMechanismClass, "mechanism", "J");
    assert(fieldID != 0);
    jMechanismType = (*env)->GetLongField(env, jMechanism, fieldID);
    ckMechanismType = jLongToCKULong(jMechanismType);
    if (ckMechanismType != ckMechanism->mechanism) {
	/* we do not have matching types, this should not occur */
	return;
    }

    ckKeyWrapSetOaepParams = (CK_KEY_WRAP_SET_OAEP_PARAMS *) ckMechanism->pParameter;
    if (ckKeyWrapSetOaepParams != NULL_PTR) {
	x = ckKeyWrapSetOaepParams->pX;
	if (x != NULL_PTR) {
	    /* get pParameter */
	    fieldID = (*env)->GetFieldID(env, jMechanismClass, "pParameter", "Ljava/lang/Object;");
	    assert(fieldID != 0);
	    jParameter = (*env)->GetObjectField(env, jMechanism, fieldID);

	    /* copy back the bBC */
	    fieldID = (*env)->GetFieldID(env, jSetParamsClass, "bBC", "B");
	    assert(fieldID != 0);
	    (*env)->SetByteField(env, jParameter, fieldID, ckKeyWrapSetOaepParams->bBC);

	    /* copy back the pX */
	    fieldID = (*env)->GetFieldID(env, jSetParamsClass, "pX", "[B");
	    assert(fieldID != 0);
	    jx = (*env)->GetObjectField(env, jParameter, fieldID);

	    if (jx != NULL_PTR) {
		jxLength = (*env)->GetArrayLength(env, jx);
		jxBytes = (*env)->GetByteArrayElements(env, jx, NULL_PTR);
		/* copy the bytes to the Java buffer */
		for (i = 0; i < jxLength; i++) {
		    jxBytes[i] = ckByteToJByte(x[i]);
		}
		/* copy back the Java buffer to the object */
		(*env)->ReleaseByteArrayElements(env, jx, jxBytes, 0);
	    }
	}
    }
}

/*
 * Copy back the client version information from the native
 * structure to the Java object. This is only used for the
 * CKM_SSL3_MASTER_KEY_DERIVE mechanism when used for deriving a key.
 *
 */
void copyBackClientVersion(JNIEnv * env, CK_MECHANISM * ckMechanism, jobject jMechanism)
{
    jclass jMechanismClass = (*env)->FindClass(env, CLASS_MECHANISM);
    jclass jSSL3MasterKeyDeriveParamsClass = (*env)->FindClass(env, CLASS_SSL3_MASTER_KEY_DERIVE_PARAMS);
    jclass jVersionClass = (*env)->FindClass(env, CLASS_VERSION);
    CK_SSL3_MASTER_KEY_DERIVE_PARAMS *ckSSL3MasterKeyDeriveParams;
    CK_VERSION *ckVersion;
    jfieldID fieldID;
    CK_MECHANISM_TYPE ckMechanismType;
    jlong jMechanismType;
    jobject jSSL3MasterKeyDeriveParams;
    jobject jVersion;

    /* get mechanism */
    fieldID = (*env)->GetFieldID(env, jMechanismClass, "mechanism", "J");
    assert(fieldID != 0);
    jMechanismType = (*env)->GetLongField(env, jMechanism, fieldID);
    ckMechanismType = jLongToCKULong(jMechanismType);
    if (ckMechanismType != ckMechanism->mechanism) {
	/* we do not have matching types, this should not occur */
	return;
    }

    /* get the native CK_SSL3_MASTER_KEY_DERIVE_PARAMS */
    ckSSL3MasterKeyDeriveParams = (CK_SSL3_MASTER_KEY_DERIVE_PARAMS *) ckMechanism->pParameter;
    if (ckSSL3MasterKeyDeriveParams != NULL_PTR) {
	/* get the native CK_VERSION */
	ckVersion = ckSSL3MasterKeyDeriveParams->pVersion;
	if (ckVersion != NULL_PTR) {
	    /* get the Java CK_SSL3_MASTER_KEY_DERIVE_PARAMS (pParameter) */
	    fieldID = (*env)->GetFieldID(env, jMechanismClass, "pParameter", "Ljava/lang/Object;");
	    assert(fieldID != 0);
	    jSSL3MasterKeyDeriveParams = (*env)->GetObjectField(env, jMechanism, fieldID);

	    /* get the Java CK_VERSION */
	    fieldID = (*env)->GetFieldID(env, jSSL3MasterKeyDeriveParamsClass, "pVersion", "L" CLASS_VERSION ";");
	    assert(fieldID != 0);
	    jVersion = (*env)->GetObjectField(env, jSSL3MasterKeyDeriveParams, fieldID);

	    /* now copy back the version from the native structure to the Java structure */

	    /* copy back the major version */
	    fieldID = (*env)->GetFieldID(env, jVersionClass, "major", "B");
	    assert(fieldID != 0);
	    (*env)->SetByteField(env, jVersion, fieldID, ckByteToJByte(ckVersion->major));

	    /* copy back the minor version */
	    fieldID = (*env)->GetFieldID(env, jVersionClass, "minor", "B");
	    assert(fieldID != 0);
	    (*env)->SetByteField(env, jVersion, fieldID, ckByteToJByte(ckVersion->minor));
	}
    }
}

/*
 * Copy back the derived keys and initialization vectors from the native
 * structure to the Java object. This is only used for the
 * CKM_SSL3_KEY_AND_MAC_DERIVE mechanism when used for deriving a key.
 *
 */
void copyBackSSLKeyMatParams(JNIEnv * env, CK_MECHANISM * ckMechanism, jobject jMechanism)
{
    jclass jMechanismClass = (*env)->FindClass(env, CLASS_MECHANISM);
    jclass jSSL3KeyMatParamsClass = (*env)->FindClass(env, CLASS_SSL3_KEY_MAT_PARAMS);
    jclass jSSL3KeyMatOutClass = (*env)->FindClass(env, CLASS_SSL3_KEY_MAT_OUT);
    CK_SSL3_KEY_MAT_PARAMS *ckSSL3KeyMatParam;
    CK_SSL3_KEY_MAT_OUT *ckSSL3KeyMatOut;
    jfieldID fieldID;
    CK_MECHANISM_TYPE ckMechanismType;
    jlong jMechanismType;
    CK_BYTE_PTR iv;
    jobject jSSL3KeyMatParam;
    jobject jSSL3KeyMatOut;
    jobject jIV;
    jint jLength;
    jbyte *jBytes;
    int i;

    /* get mechanism */
    fieldID = (*env)->GetFieldID(env, jMechanismClass, "mechanism", "J");
    assert(fieldID != 0);
    jMechanismType = (*env)->GetLongField(env, jMechanism, fieldID);
    ckMechanismType = jLongToCKULong(jMechanismType);
    if (ckMechanismType != ckMechanism->mechanism) {
	/* we do not have matching types, this should not occur */
	return;
    }

    /* get the native CK_SSL3_KEY_MAT_PARAMS */
    ckSSL3KeyMatParam = (CK_SSL3_KEY_MAT_PARAMS *) ckMechanism->pParameter;
    if (ckSSL3KeyMatParam != NULL_PTR) {
	/* get the native CK_SSL3_KEY_MAT_OUT */
	ckSSL3KeyMatOut = ckSSL3KeyMatParam->pReturnedKeyMaterial;
	if (ckSSL3KeyMatOut != NULL_PTR) {
	    /* get the Java CK_SSL3_KEY_MAT_PARAMS (pParameter) */
	    fieldID = (*env)->GetFieldID(env, jMechanismClass, "pParameter", "Ljava/lang/Object;");
	    assert(fieldID != 0);
	    jSSL3KeyMatParam = (*env)->GetObjectField(env, jMechanism, fieldID);

	    /* get the Java CK_SSL3_KEY_MAT_OUT */
	    fieldID =
		(*env)->GetFieldID(env, jSSL3KeyMatParamsClass, "pReturnedKeyMaterial", "L" CLASS_SSL3_KEY_MAT_OUT ";");
	    assert(fieldID != 0);
	    jSSL3KeyMatOut = (*env)->GetObjectField(env, jSSL3KeyMatParam, fieldID);

	    /* now copy back all the key handles and the initialization vectors */
	    /* copy back client MAC secret handle */
	    fieldID = (*env)->GetFieldID(env, jSSL3KeyMatOutClass, "hClientMacSecret", "J");
	    assert(fieldID != 0);
	    (*env)->SetLongField(env, jSSL3KeyMatOut, fieldID, ckULongToJLong(ckSSL3KeyMatOut->hClientMacSecret));

	    /* copy back server MAC secret handle */
	    fieldID = (*env)->GetFieldID(env, jSSL3KeyMatOutClass, "hServerMacSecret", "J");
	    assert(fieldID != 0);
	    (*env)->SetLongField(env, jSSL3KeyMatOut, fieldID, ckULongToJLong(ckSSL3KeyMatOut->hServerMacSecret));

	    /* copy back client secret key handle */
	    fieldID = (*env)->GetFieldID(env, jSSL3KeyMatOutClass, "hClientKey", "J");
	    assert(fieldID != 0);
	    (*env)->SetLongField(env, jSSL3KeyMatOut, fieldID, ckULongToJLong(ckSSL3KeyMatOut->hClientKey));

	    /* copy back server secret key handle */
	    fieldID = (*env)->GetFieldID(env, jSSL3KeyMatOutClass, "hServerKey", "J");
	    assert(fieldID != 0);
	    (*env)->SetLongField(env, jSSL3KeyMatOut, fieldID, ckULongToJLong(ckSSL3KeyMatOut->hServerKey));

	    /* copy back the client IV */
	    fieldID = (*env)->GetFieldID(env, jSSL3KeyMatOutClass, "pIVClient", "[B");
	    assert(fieldID != 0);
	    jIV = (*env)->GetObjectField(env, jSSL3KeyMatOut, fieldID);
	    iv = ckSSL3KeyMatOut->pIVClient;

	    if (jIV != NULL_PTR) {
		jLength = (*env)->GetArrayLength(env, jIV);
		jBytes = (*env)->GetByteArrayElements(env, jIV, NULL_PTR);
		/* copy the bytes to the Java buffer */
		for (i = 0; i < jLength; i++) {
		    jBytes[i] = ckByteToJByte(iv[i]);
		}
		/* copy back the Java buffer to the object */
		(*env)->ReleaseByteArrayElements(env, jIV, jBytes, 0);
	    }

	    /* copy back the server IV */
	    fieldID = (*env)->GetFieldID(env, jSSL3KeyMatOutClass, "pIVServer", "[B");
	    assert(fieldID != 0);
	    jIV = (*env)->GetObjectField(env, jSSL3KeyMatOut, fieldID);
	    iv = ckSSL3KeyMatOut->pIVServer;

	    if (jIV != NULL_PTR) {
		jLength = (*env)->GetArrayLength(env, jIV);
		jBytes = (*env)->GetByteArrayElements(env, jIV, NULL_PTR);
		/* copy the bytes to the Java buffer */
		for (i = 0; i < jLength; i++) {
		    jBytes[i] = ckByteToJByte(iv[i]);
		}
		/* copy back the Java buffer to the object */
		(*env)->ReleaseByteArrayElements(env, jIV, jBytes, 0);
	    }
	}
    }
}

/*
 * converts the Java CK_SSL3_MASTER_KEY_DERIVE_PARAMS object to a
 * CK_SSL3_MASTER_KEY_DERIVE_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_SSL3_MASTER_KEY_DERIVE_PARAMS object to convert
 * @return - the new CK_SSL3_MASTER_KEY_DERIVE_PARAMS structure
 */
CK_SSL3_MASTER_KEY_DERIVE_PARAMS jSsl3MasterKeyDeriveParamToCKSsl3MasterKeyDeriveParam(JNIEnv * env, jobject jParam)
{
    jclass jSsl3MasterKeyDeriveParamsClass = (*env)->FindClass(env, CLASS_SSL3_MASTER_KEY_DERIVE_PARAMS);
    CK_SSL3_MASTER_KEY_DERIVE_PARAMS ckParam;
    jfieldID fieldID;
    jobject jObject;
    jclass jSsl3RandomDataClass;
    jobject jRandomInfo;

    /* get RandomInfo */
    jSsl3RandomDataClass = (*env)->FindClass(env, CLASS_SSL3_RANDOM_DATA);
    fieldID =
	(*env)->GetFieldID(env, jSsl3MasterKeyDeriveParamsClass, "RandomInfo", CLASS_NAME(CLASS_SSL3_RANDOM_DATA));
    assert(fieldID != 0);
    jRandomInfo = (*env)->GetObjectField(env, jParam, fieldID);

    /* get pClientRandom and ulClientRandomLength out of RandomInfo */
    fieldID = (*env)->GetFieldID(env, jSsl3RandomDataClass, "pClientRandom", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jRandomInfo, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.RandomInfo.pClientRandom), &(ckParam.RandomInfo.ulClientRandomLen));

    /* get pServerRandom and ulServerRandomLength out of RandomInfo */
    fieldID = (*env)->GetFieldID(env, jSsl3RandomDataClass, "pServerRandom", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jRandomInfo, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.RandomInfo.pServerRandom), &(ckParam.RandomInfo.ulServerRandomLen));

    /* get pVersion */
    fieldID = (*env)->GetFieldID(env, jSsl3MasterKeyDeriveParamsClass, "pVersion", CLASS_NAME(CLASS_VERSION));
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    ckParam.pVersion = jVersionToCKVersionPtr(env, jObject);

    return ckParam;
}

/*
 * converts the Java CK_SSL3_KEY_MAT_PARAMS object to a CK_SSL3_KEY_MAT_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_SSL3_KEY_MAT_PARAMS object to convert
 * @return - the new CK_SSL3_KEY_MAT_PARAMS structure
 */
CK_SSL3_KEY_MAT_PARAMS jSsl3KeyMatParamToCKSsl3KeyMatParam(JNIEnv * env, jobject jParam)
{
    jclass jSsl3KeyMatParamsClass = (*env)->FindClass(env, CLASS_SSL3_KEY_MAT_PARAMS);
    CK_SSL3_KEY_MAT_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;
    jboolean jBoolean;
    jobject jObject;
    jobject jRandomInfo;
    jobject jReturnedKeyMaterial;
    jclass jSsl3RandomDataClass;
    jclass jSsl3KeyMatOutClass;
    CK_ULONG ckTemp;

    /* get ulMacSizeInBits */
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatParamsClass, "ulMacSizeInBits", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulMacSizeInBits = jLongToCKULong(jLong);

    /* get ulKeySizeInBits */
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatParamsClass, "ulKeySizeInBits", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulKeySizeInBits = jLongToCKULong(jLong);

    /* get ulIVSizeInBits */
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatParamsClass, "ulIVSizeInBits", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulIVSizeInBits = jLongToCKULong(jLong);

    /* get bIsExport */
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatParamsClass, "bIsExport", "Z");
    assert(fieldID != 0);
    jBoolean = (*env)->GetBooleanField(env, jParam, fieldID);
    ckParam.bIsExport = jBooleanToCKBBool(jBoolean);

    /* get RandomInfo */
    jSsl3RandomDataClass = (*env)->FindClass(env, CLASS_SSL3_RANDOM_DATA);
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatParamsClass, "RandomInfo", CLASS_NAME(CLASS_SSL3_RANDOM_DATA));
    assert(fieldID != 0);
    jRandomInfo = (*env)->GetObjectField(env, jParam, fieldID);

    /* get pClientRandom and ulClientRandomLength out of RandomInfo */
    fieldID = (*env)->GetFieldID(env, jSsl3RandomDataClass, "pClientRandom", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jRandomInfo, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.RandomInfo.pClientRandom), &(ckParam.RandomInfo.ulClientRandomLen));

    /* get pServerRandom and ulServerRandomLength out of RandomInfo */
    fieldID = (*env)->GetFieldID(env, jSsl3RandomDataClass, "pServerRandom", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jRandomInfo, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.RandomInfo.pServerRandom), &(ckParam.RandomInfo.ulServerRandomLen));

    /* get pReturnedKeyMaterial */
    jSsl3KeyMatOutClass = (*env)->FindClass(env, CLASS_SSL3_KEY_MAT_OUT);
    fieldID =
	(*env)->GetFieldID(env, jSsl3KeyMatParamsClass, "pReturnedKeyMaterial", CLASS_NAME(CLASS_SSL3_KEY_MAT_OUT));
    assert(fieldID != 0);
    jReturnedKeyMaterial = (*env)->GetObjectField(env, jParam, fieldID);

    /* allocate memory for pRetrunedKeyMaterial */
    ckParam.pReturnedKeyMaterial = (CK_SSL3_KEY_MAT_OUT_PTR) malloc(sizeof(CK_SSL3_KEY_MAT_OUT));
    if (ckParam.pReturnedKeyMaterial == NULL_PTR) {
	throwOutOfMemoryError(env);
	return ckParam;
    }

    /* get hClientMacSecret out of pReturnedKeyMaterial */
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatOutClass, "hClientMacSecret", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jReturnedKeyMaterial, fieldID);
    ckParam.pReturnedKeyMaterial->hClientMacSecret = jLongToCKULong(jLong);

    /* get hServerMacSecret out of pReturnedKeyMaterial */
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatOutClass, "hServerMacSecret", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jReturnedKeyMaterial, fieldID);
    ckParam.pReturnedKeyMaterial->hServerMacSecret = jLongToCKULong(jLong);

    /* get hClientKey out of pReturnedKeyMaterial */
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatOutClass, "hClientKey", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jReturnedKeyMaterial, fieldID);
    ckParam.pReturnedKeyMaterial->hClientKey = jLongToCKULong(jLong);

    /* get hServerKey out of pReturnedKeyMaterial */
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatOutClass, "hServerKey", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jReturnedKeyMaterial, fieldID);
    ckParam.pReturnedKeyMaterial->hServerKey = jLongToCKULong(jLong);

    /* get pIVClient out of pReturnedKeyMaterial */
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatOutClass, "pIVClient", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jReturnedKeyMaterial, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pReturnedKeyMaterial->pIVClient), &ckTemp);

    /* get pIVServer out of pReturnedKeyMaterial */
    fieldID = (*env)->GetFieldID(env, jSsl3KeyMatOutClass, "pIVServer", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jReturnedKeyMaterial, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pReturnedKeyMaterial->pIVServer), &ckTemp);

    return ckParam;
}

/*
 * converts the Java CK_KEY_DERIVATION_STRING_DATA object to a
 * CK_KEY_DERIVATION_STRING_DATA structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_KEY_DERIVATION_STRING_DATA object to convert
 * @return - the new CK_KEY_DERIVATION_STRING_DATA structure
 */
CK_KEY_DERIVATION_STRING_DATA jKeyDerivationStringDataToCKKeyDerivationStringData(JNIEnv * env, jobject jParam)
{
    jclass jKeyDerivationStringDataClass = (*env)->FindClass(env, CLASS_KEY_DERIVATION_STRING_DATA);
    CK_KEY_DERIVATION_STRING_DATA ckParam;
    jfieldID fieldID;
    jobject jObject;

    /* get pData */
    fieldID = (*env)->GetFieldID(env, jKeyDerivationStringDataClass, "pData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pData), &(ckParam.ulLen));

    return ckParam;
}

/*
 * converts the Java CK_RSA_PKCS_PSS_PARAMS object to a CK_RSA_PKCS_PSS_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_RSA_PKCS_PSS_PARAMS object to convert
 * @return - the new CK_RSA_PKCS_PSS_PARAMS structure
 */
CK_RSA_PKCS_PSS_PARAMS jRsaPkcsPssParamToCKRsaPkcsPssParam(JNIEnv * env, jobject jParam)
{
    jclass jRsaPkcsPssParamsClass = (*env)->FindClass(env, CLASS_RSA_PKCS_PSS_PARAMS);
    CK_RSA_PKCS_PSS_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;

    /* get hashAlg */
    fieldID = (*env)->GetFieldID(env, jRsaPkcsPssParamsClass, "hashAlg", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.hashAlg = jLongToCKULong(jLong);

    /* get mgf */
    fieldID = (*env)->GetFieldID(env, jRsaPkcsPssParamsClass, "mgf", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.mgf = jLongToCKULong(jLong);

    /* get sLen */
    fieldID = (*env)->GetFieldID(env, jRsaPkcsPssParamsClass, "sLen", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.sLen = jLongToCKULong(jLong);

    return ckParam;
}

/*
 * converts the Java CK_ECDH1_DERIVE_PARAMS object to a CK_ECDH1_DERIVE_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_ECDH1_DERIVE_PARAMS object to convert
 * @return - the new CK_ECDH1_DERIVE_PARAMS structure
 */
CK_ECDH1_DERIVE_PARAMS jEcdh1DeriveParamToCKEcdh1DeriveParam(JNIEnv * env, jobject jParam)
{
    jclass jEcdh1DeriveParamsClass = (*env)->FindClass(env, CLASS_ECDH1_DERIVE_PARAMS);
    CK_ECDH1_DERIVE_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;
    jobject jObject;

    /* get kdf */
    fieldID = (*env)->GetFieldID(env, jEcdh1DeriveParamsClass, "kdf", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.kdf = jLongToCKULong(jLong);

    /* get pSharedData and ulSharedDataLen */
    fieldID = (*env)->GetFieldID(env, jEcdh1DeriveParamsClass, "pSharedData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pSharedData), &(ckParam.ulSharedDataLen));

    /* get pPublicData and ulPublicDataLen */
    fieldID = (*env)->GetFieldID(env, jEcdh1DeriveParamsClass, "pPublicData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pPublicData), &(ckParam.ulPublicDataLen));

    return ckParam;
}

/*
 * converts the Java CK_ECDH2_DERIVE_PARAMS object to a CK_ECDH2_DERIVE_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_ECDH2_DERIVE_PARAMS object to convert
 * @return - the new CK_ECDH2_DERIVE_PARAMS structure
 */
CK_ECDH2_DERIVE_PARAMS jEcdh2DeriveParamToCKEcdh2DeriveParam(JNIEnv * env, jobject jParam)
{
    jclass jEcdh2DeriveParamsClass = (*env)->FindClass(env, CLASS_ECDH2_DERIVE_PARAMS);
    CK_ECDH2_DERIVE_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;
    jobject jObject;

    /* get kdf */
    fieldID = (*env)->GetFieldID(env, jEcdh2DeriveParamsClass, "kdf", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.kdf = jLongToCKULong(jLong);

    /* get pSharedData and ulSharedDataLen */
    fieldID = (*env)->GetFieldID(env, jEcdh2DeriveParamsClass, "pSharedData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pSharedData), &(ckParam.ulSharedDataLen));

    /* get pPublicData and ulPublicDataLen */
    fieldID = (*env)->GetFieldID(env, jEcdh2DeriveParamsClass, "pPublicData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pPublicData), &(ckParam.ulPublicDataLen));

    /* get ulPrivateDataLen */
    fieldID = (*env)->GetFieldID(env, jEcdh2DeriveParamsClass, "ulPrivateDataLen", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulPrivateDataLen = jLongToCKULong(jLong);

    /* get hPrivateData */
    fieldID = (*env)->GetFieldID(env, jEcdh2DeriveParamsClass, "hPrivateData", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.hPrivateData = jLongToCKULong(jLong);

    /* get pPublicData2 and ulPublicDataLen2 */
    fieldID = (*env)->GetFieldID(env, jEcdh2DeriveParamsClass, "pPublicData2", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pPublicData2), &(ckParam.ulPublicDataLen2));

    return ckParam;
}

/*
 * converts the Java CK_X9_42_DH1_DERIVE_PARAMS object to a CK_X9_42_DH1_DERIVE_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_X9_42_DH1_DERIVE_PARAMS object to convert
 * @return - the new CK_X9_42_DH1_DERIVE_PARAMS structure
 */
CK_X9_42_DH1_DERIVE_PARAMS jX942Dh1DeriveParamToCKX942Dh1DeriveParam(JNIEnv * env, jobject jParam)
{
    jclass jX942Dh1DeriveParamsClass = (*env)->FindClass(env, CLASS_X9_42_DH1_DERIVE_PARAMS);
    CK_X9_42_DH1_DERIVE_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;
    jobject jObject;

    /* get kdf */
    fieldID = (*env)->GetFieldID(env, jX942Dh1DeriveParamsClass, "kdf", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.kdf = jLongToCKULong(jLong);

    /* get pOtherInfo and ulOtherInfoLen */
    fieldID = (*env)->GetFieldID(env, jX942Dh1DeriveParamsClass, "pOtherInfo", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pOtherInfo), &(ckParam.ulOtherInfoLen));

    /* get pPublicData and ulPublicDataLen */
    fieldID = (*env)->GetFieldID(env, jX942Dh1DeriveParamsClass, "pPublicData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pPublicData), &(ckParam.ulPublicDataLen));

    return ckParam;
}

/*
 * converts the Java CK_X9_42_DH2_DERIVE_PARAMS object to a CK_X9_42_DH2_DERIVE_PARAMS structure
 *
 * @param env - used to call JNI functions to get the Java classes and objects
 * @param jParam - the Java CK_X9_42_DH2_DERIVE_PARAMS object to convert
 * @return - the new CK_X9_42_DH2_DERIVE_PARAMS structure
 */
CK_X9_42_DH2_DERIVE_PARAMS jX942Dh2DeriveParamToCKX942Dh2DeriveParam(JNIEnv * env, jobject jParam)
{
    jclass jX942Dh2DeriveParamsClass = (*env)->FindClass(env, CLASS_X9_42_DH2_DERIVE_PARAMS);
    CK_X9_42_DH2_DERIVE_PARAMS ckParam;
    jfieldID fieldID;
    jlong jLong;
    jobject jObject;

    /* get kdf */
    fieldID = (*env)->GetFieldID(env, jX942Dh2DeriveParamsClass, "kdf", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.kdf = jLongToCKULong(jLong);

    /* get pOtherInfo and ulOtherInfoLen */
    fieldID = (*env)->GetFieldID(env, jX942Dh2DeriveParamsClass, "pOtherInfo", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pOtherInfo), &(ckParam.ulOtherInfoLen));

    /* get pPublicData and ulPublicDataLen */
    fieldID = (*env)->GetFieldID(env, jX942Dh2DeriveParamsClass, "pPublicData", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pPublicData), &(ckParam.ulPublicDataLen));

    /* get ulPrivateDataLen */
    fieldID = (*env)->GetFieldID(env, jX942Dh2DeriveParamsClass, "ulPrivateDataLen", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.ulPrivateDataLen = jLongToCKULong(jLong);

    /* get hPrivateData */
    fieldID = (*env)->GetFieldID(env, jX942Dh2DeriveParamsClass, "hPrivateData", "J");
    assert(fieldID != 0);
    jLong = (*env)->GetLongField(env, jParam, fieldID);
    ckParam.hPrivateData = jLongToCKULong(jLong);

    /* get pPublicData2 and ulPublicDataLen2 */
    fieldID = (*env)->GetFieldID(env, jX942Dh2DeriveParamsClass, "pPublicData2", "[B");
    assert(fieldID != 0);
    jObject = (*env)->GetObjectField(env, jParam, fieldID);
    jByteArrayToCKByteArray(env, jObject, &(ckParam.pPublicData2), &(ckParam.ulPublicDataLen2));

    return ckParam;
}
