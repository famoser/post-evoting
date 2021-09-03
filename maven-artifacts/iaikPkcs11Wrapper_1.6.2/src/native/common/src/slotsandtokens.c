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
/* for slot and token related functions                                       */
/* ************************************************************************** */

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetSlotList
 * Signature: (Z)[J
 * Parametermapping:                    *PKCS11*
 * @param   jboolean jTokenPresent      CK_BBOOL tokenPresent
 * @return  jlongArray jSlotList        CK_SLOT_ID_PTR pSlotList
 *                                      CK_ULONG_PTR pulCount
 */
JNIEXPORT jlongArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetSlotList
    (JNIEnv * env, jobject obj, jboolean jTokenPresent) {
    CK_ULONG ckTokenNumber;
    CK_SLOT_ID_PTR ckpSlotList;
    CK_BBOOL ckTokenPresent;
    jlongArray jSlotList;
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

    ckTokenPresent = jBooleanToCKBBool(jTokenPresent);

    rv = (*ckpFunctions->C_GetSlotList) (ckTokenPresent, NULL_PTR, &ckTokenNumber);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    if (ckTokenNumber != 0) {	/* only make the second call, if the number is not zero */
	ckpSlotList = (CK_SLOT_ID_PTR) malloc(ckTokenNumber * sizeof(CK_SLOT_ID));
	if (ckpSlotList == NULL_PTR && ckTokenNumber != 0) {
	    throwOutOfMemoryError(env);
	    return NULL_PTR;
	}

	rv = (*ckpFunctions->C_GetSlotList) (ckTokenPresent, ckpSlotList, &ckTokenNumber);

	if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	    jSlotList = ckULongArrayToJLongArray(env, ckpSlotList, ckTokenNumber);
	else
	    jSlotList = NULL_PTR;

	free(ckpSlotList);
    } else {
	jSlotList = ckULongArrayToJLongArray(env, NULL_PTR, ckTokenNumber);
    }

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jSlotList;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetSlotInfo
 * Signature: (J)Liaik/pkcs/pkcs11/wrapper/CK_SLOT_INFO;
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @return  jobject jSlotInfoObject     CK_SLOT_INFO_PTR pInfo
 */
JNIEXPORT jobject JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetSlotInfo
    (JNIEnv * env, jobject obj, jlong jSlotID) {
    CK_SLOT_ID ckSlotID;
    CK_SLOT_INFO ckSlotInfo;
    jobject jSlotInfoObject;
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

    ckSlotID = jLongToCKULong(jSlotID);

    rv = (*ckpFunctions->C_GetSlotInfo) (ckSlotID, &ckSlotInfo);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    jSlotInfoObject = ckSlotInfoPtrToJSlotInfo(env, &ckSlotInfo);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jSlotInfoObject;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetTokenInfo
 * Signature: (J)Liaik/pkcs/pkcs11/wrapper/CK_TOKEN_INFO;
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @return  jobject jInfoTokenObject    CK_TOKEN_INFO_PTR pInfo
 */
JNIEXPORT jobject JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetTokenInfo
    (JNIEnv * env, jobject obj, jlong jSlotID) {
    CK_SLOT_ID ckSlotID;
    CK_TOKEN_INFO ckTokenInfo;
    jobject jInfoTokenObject;
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

    ckSlotID = jLongToCKULong(jSlotID);

    rv = (*ckpFunctions->C_GetTokenInfo) (ckSlotID, &ckTokenInfo);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    jInfoTokenObject = ckTokenInfoPtrToJTokenInfo(env, &ckTokenInfo);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jInfoTokenObject;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_WaitForSlotEvent
 * Signature: (JLjava/lang/Object;)J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jFlags                CK_FLAGS flags
 * @param   jobject jReserved           CK_VOID_PTR pReserved
 * @return  jlong jSlotID               CK_SLOT_ID_PTR pSlot
 */
JNIEXPORT jlong JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1WaitForSlotEvent
    (JNIEnv * env, jobject obj, jlong jFlags, jobject jReserved) {
    CK_FLAGS ckFlags;
    CK_SLOT_ID ckSlotID;
    jlong jSlotID;
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

    ckFlags = jLongToCKULong(jFlags);

    rv = (*ckpFunctions->C_WaitForSlotEvent) (ckFlags, &ckSlotID, NULL_PTR);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return 0L;
    }

    jSlotID = ckULongToJLong(ckSlotID);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jSlotID;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetMechanismList
 * Signature: (J)[J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @return  jlongArray jMechanismList   CK_MECHANISM_TYPE_PTR pMechanismList
 *                                      CK_ULONG_PTR pulCount
 */
JNIEXPORT jlongArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetMechanismList
    (JNIEnv * env, jobject obj, jlong jSlotID) {
    CK_SLOT_ID ckSlotID;
    CK_ULONG ckMechanismNumber;
    CK_MECHANISM_TYPE_PTR ckpMechanismList;
    jlongArray jMechanismList;
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

    ckSlotID = jLongToCKULong(jSlotID);

    rv = (*ckpFunctions->C_GetMechanismList) (ckSlotID, NULL_PTR, &ckMechanismNumber);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpMechanismList = (CK_MECHANISM_TYPE_PTR) malloc(ckMechanismNumber * sizeof(CK_MECHANISM_TYPE));
    if (ckpMechanismList == NULL_PTR && ckMechanismNumber != 0) {
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_GetMechanismList) (ckSlotID, ckpMechanismList, &ckMechanismNumber);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jMechanismList = ckULongArrayToJLongArray(env, ckpMechanismList, ckMechanismNumber);
    else
	jMechanismList = NULL_PTR;

    free(ckpMechanismList);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jMechanismList;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetMechanismInfo
 * Signature: (JJ)Liaik/pkcs/pkcs11/wrapper/CK_MECHANISM_INFO;
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @param   jlong jType                 CK_MECHANISM_TYPE type
 * @return  jobject jMechanismInfo      CK_MECHANISM_INFO_PTR pInfo
 */
JNIEXPORT jobject JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetMechanismInfo
    (JNIEnv * env, jobject obj, jlong jSlotID, jlong jType) {
    CK_SLOT_ID ckSlotID;
    CK_MECHANISM_TYPE ckMechanismType;
    CK_MECHANISM_INFO ckMechanismInfo;
    jobject jMechanismInfo;
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

    ckSlotID = jLongToCKULong(jSlotID);
    ckMechanismType = jLongToCKULong(jType);

    rv = (*ckpFunctions->C_GetMechanismInfo) (ckSlotID, ckMechanismType, &ckMechanismInfo);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    jMechanismInfo = ckMechanismInfoPtrToJMechanismInfo(env, &ckMechanismInfo);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jMechanismInfo;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_InitToken
 * Signature: (J[C[CZ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @param   jcharArray jPin             CK_UTF8CHAR_PTR pPin
 *                                      CK_ULONG ulPinLen
 * @param   jcharArray jLabel           CK_UTF8CHAR_PTR pLabel
 * @param	jboolean jUseUtf8			if new Pin shall be saved as UTF8 encoding
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1InitToken
    (JNIEnv * env, jobject obj, jlong jSlotID, jcharArray jPin, jcharArray jLabel, jboolean jUseUtf8) {
    CK_SLOT_ID ckSlotID;
    CK_CHAR_PTR ckpPin = NULL_PTR;
    CK_UTF8CHAR_PTR ckpLabel = NULL_PTR;
    CK_ULONG ckPinLength;
    CK_ULONG ckLabelLength;
    CK_RV rv;
    CK_BBOOL ckUseUtf8;
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

    ckSlotID = jLongToCKULong(jSlotID);
    ckUseUtf8 = jBooleanToCKBBool(jUseUtf8);
    if (ckUseUtf8 == TRUE) {
	if (jCharArrayToCKUTF8CharArray(env, jPin, &ckpPin, &ckPinLength)) {
	    return;
	}
	if (jCharArrayToCKUTF8CharArray(env, jLabel, &ckpLabel, &ckLabelLength)) {
	    return;
	}
    } else {
	if (jCharArrayToCKCharArray(env, jPin, &ckpPin, &ckPinLength)) {
	    return;
	}
	if (jCharArrayToCKCharArray(env, jLabel, &ckpLabel, &ckLabelLength)) {
	    return;
	}
    }

    rv = (*ckpFunctions->C_InitToken) (ckSlotID, ckpPin, ckPinLength, ckpLabel);

    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	TRACE1(tag_info, __FUNCTION__, "InitToken return code: %u", (unsigned int)rv);

    free(ckpPin);
    free(ckpLabel);
    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_InitPIN
 * Signature: (J[CZ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE
 * @param   jcharArray jPin             CK_CHAR_PTR pPin
 *                                      CK_ULONG ulPinLen
 * @param	jboolean jUseUtf8		if new Pin shall be saved as UTF8 encoding
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1InitPIN
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jcharArray jPin, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_CHAR_PTR ckpPin = NULL_PTR;
    CK_ULONG ckPinLength;
    CK_RV rv;
    CK_BBOOL ckUseUtf8;
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
    ckUseUtf8 = jBooleanToCKBBool(jUseUtf8);
    if (ckUseUtf8 == TRUE) {
	if (jCharArrayToCKUTF8CharArray(env, jPin, &ckpPin, &ckPinLength)) {
	    return;
	}
    } else {
	if (jCharArrayToCKCharArray(env, jPin, &ckpPin, &ckPinLength)) {
	    return;
	}
    }

    rv = (*ckpFunctions->C_InitPIN) (ckSessionHandle, ckpPin, ckPinLength);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    free(ckpPin);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_SetPIN
 * Signature: (J[C[CZ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jcharArray jOldPin          CK_CHAR_PTR pOldPin
 *                                      CK_ULONG ulOldLen
 * @param   jcharArray jNewPin          CK_CHAR_PTR pNewPin
 *                                      CK_ULONG ulNewLen
 * @param	jboolean jUseUtf8		if new Pin shall be saved as UTF8 encoding
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1SetPIN
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jcharArray jOldPin, jcharArray jNewPin, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_CHAR_PTR ckpOldPin = NULL_PTR;
    CK_CHAR_PTR ckpNewPin = NULL_PTR;
    CK_ULONG ckOldPinLength;
    CK_ULONG ckNewPinLength;
    CK_RV rv;
    CK_BBOOL ckUseUtf8;
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

    ckUseUtf8 = jBooleanToCKBBool(jUseUtf8);
    if (ckUseUtf8 == TRUE) {
	if (jCharArrayToCKUTF8CharArray(env, jOldPin, &ckpOldPin, &ckOldPinLength)) {
	    return;
	}
	if (jCharArrayToCKUTF8CharArray(env, jNewPin, &ckpNewPin, &ckNewPinLength)) {
	    return;
	}
    } else {
	if (jCharArrayToCKCharArray(env, jOldPin, &ckpOldPin, &ckOldPinLength)) {
	    return;
	}
	if (jCharArrayToCKCharArray(env, jNewPin, &ckpNewPin, &ckNewPinLength)) {
	    return;
	}
    }

    rv = (*ckpFunctions->C_SetPIN) (ckSessionHandle, ckpOldPin, ckOldPinLength, ckpNewPin, ckNewPinLength);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    free(ckpOldPin);
    free(ckpNewPin);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}
