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
/* for handling sessions                                                      */
/* ************************************************************************** */

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_OpenSession
 * Signature: (JJLjava/lang/Object;Liaik/pkcs/pkcs11/wrapper/CK_NOTIFY;)J
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 * @param   jlong jFlags                CK_FLAGS flags
 * @param   jobject jApplication        CK_VOID_PTR pApplication
 * @param   jobject jNotify             CK_NOTIFY Notify
 * @return  jlong jSessionHandle        CK_SESSION_HANDLE_PTR phSession
 */
JNIEXPORT jlong JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1OpenSession
    (JNIEnv * env, jobject obj, jlong jSlotID, jlong jFlags, jobject jApplication, jobject jNotify) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_SLOT_ID ckSlotID;
    CK_FLAGS ckFlags;
    CK_VOID_PTR ckpApplication;
    CK_NOTIFY ckNotify;
    jlong jSessionHandle;
    CK_RV rv;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;
#ifndef NO_CALLBACKS
    NotifyEncapsulation *notifyEncapsulation = NULL_PTR;
#endif				/* NO_CALLBACKS */

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

    ckSlotID = jLongToCKULong(jSlotID);
    ckFlags = jLongToCKULong(jFlags);

#ifndef NO_CALLBACKS
    if (jNotify != NULL_PTR) {
	notifyEncapsulation = (NotifyEncapsulation *) malloc(sizeof(NotifyEncapsulation));
	if (notifyEncapsulation == NULL_PTR) {
	    throwOutOfMemoryError(env);
	    return 0L;
	}
	notifyEncapsulation->jApplicationData = (jApplication != NULL_PTR)
	    ? (*env)->NewGlobalRef(env, jApplication)
	    : NULL_PTR;
	notifyEncapsulation->jNotifyObject = (*env)->NewGlobalRef(env, jNotify);
	ckpApplication = notifyEncapsulation;
	ckNotify = (CK_NOTIFY) & notifyCallback;
    } else {
	ckpApplication = NULL_PTR;
	ckNotify = NULL_PTR;
    }
#else
    ckpApplication = NULL_PTR;
    ckNotify = NULL_PTR;
#endif				/* NO_CALLBACKS */

    TRACE2(tag_debug, __FUNCTION__, "  slotID=%u, flags=%x", (unsigned int)ckSlotID, (unsigned int)ckFlags);

    rv = (*ckpFunctions->C_OpenSession) (ckSlotID, ckFlags, ckpApplication, ckNotify, &ckSessionHandle);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return 0L;
    }

    TRACE1(tag_info, __FUNCTION__, "got session, SessionHandle=%d", (int)ckSessionHandle);

    jSessionHandle = ckULongToJLong(ckSessionHandle);

#ifndef NO_CALLBACKS
    if (notifyEncapsulation != NULL_PTR) {
	/* store the notifyEncapsulation to enable later cleanup */
	putNotifyEntry(env, ckSessionHandle, notifyEncapsulation);
    }
#endif				/* NO_CALLBACKS */

    TRACE0(tag_call, __FUNCTION__, "exiting ");

    return jSessionHandle;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_CloseSession
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1CloseSession
    (JNIEnv * env, jobject obj, jlong jSessionHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_RV rv;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;
#ifndef NO_CALLBACKS
    NotifyEncapsulation *notifyEncapsulation;
    jobject jApplicationData;
#endif				/* NO_CALLBACKS */
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

    TRACE1(tag_info, __FUNCTION__, "going to close session with handle %d", (int)jSessionHandle);

    rv = (*ckpFunctions->C_CloseSession) (ckSessionHandle);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return;
    }

#ifndef NO_CALLBACKS
    notifyEncapsulation = removeNotifyEntry(env, ckSessionHandle);

    if (notifyEncapsulation != NULL_PTR) {
	/* there was a notify object used with this session, now dump the
	 * encapsulation object
	 */
	(*env)->DeleteGlobalRef(env, notifyEncapsulation->jNotifyObject);
	jApplicationData = notifyEncapsulation->jApplicationData;
	if (jApplicationData != NULL_PTR) {
	    (*env)->DeleteGlobalRef(env, jApplicationData);
	}
	free(notifyEncapsulation);
    }
#endif				/* NO_CALLBACKS */
    TRACE0(tag_call, __FUNCTION__, "exiting ");

}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_CloseAllSessions
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSlotID               CK_SLOT_ID slotID
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1CloseAllSessions
    (JNIEnv * env, jobject obj, jlong jSlotID) {
    CK_SLOT_ID ckSlotID;
    CK_RV rv;
    ModuleData *moduleData;
    CK_FUNCTION_LIST_PTR ckpFunctions;
#ifndef NO_CALLBACKS
    NotifyEncapsulation *notifyEncapsulation;
    jobject jApplicationData;
#endif				/* NO_CALLBACKS */
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

    rv = (*ckpFunctions->C_CloseAllSessions) (ckSlotID);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return;
    }

#ifndef NO_CALLBACKS
    /* Remove all notify callback helper objects. */
    while ((notifyEncapsulation = removeFirstNotifyEntry(env)) != NULL_PTR) {
	/* there was a notify object used with this session, now dump the
	 * encapsulation object
	 */
	(*env)->DeleteGlobalRef(env, notifyEncapsulation->jNotifyObject);
	jApplicationData = notifyEncapsulation->jApplicationData;
	if (jApplicationData != NULL_PTR) {
	    (*env)->DeleteGlobalRef(env, jApplicationData);
	}
	free(notifyEncapsulation);
    }
#endif				/* NO_CALLBACKS */
    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetSessionInfo
 * Signature: (J)Liaik/pkcs/pkcs11/wrapper/CK_SESSION_INFO;
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @return  jobject jSessionInfo        CK_SESSION_INFO_PTR pInfo
 */
JNIEXPORT jobject JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetSessionInfo
    (JNIEnv * env, jobject obj, jlong jSessionHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_SESSION_INFO ckSessionInfo;
    jobject jSessionInfo;
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

    rv = (*ckpFunctions->C_GetSessionInfo) (ckSessionHandle, &ckSessionInfo);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    jSessionInfo = ckSessionInfoPtrToJSessionInfo(env, &ckSessionInfo);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jSessionInfo;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_GetOperationState
 * Signature: (J)[B
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @return  jbyteArray jState           CK_BYTE_PTR pOperationState
 *                                      CK_ULONG_PTR pulOperationStateLen
 */
JNIEXPORT jbyteArray JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1GetOperationState
    (JNIEnv * env, jobject obj, jlong jSessionHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpState;
    CK_ULONG ckStateLength;
    jbyteArray jState;
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

    rv = (*ckpFunctions->C_GetOperationState) (ckSessionHandle, NULL_PTR, &ckStateLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) != CK_ASSERT_OK) {
	return NULL_PTR;
    }

    ckpState = (CK_BYTE_PTR) malloc(ckStateLength);
    if (ckpState == NULL_PTR && ckStateLength != 0) {
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    rv = (*ckpFunctions->C_GetOperationState) (ckSessionHandle, ckpState, &ckStateLength);
    if (ckAssertReturnValueOK(env, rv, __FUNCTION__) == CK_ASSERT_OK)
	jState = ckByteArrayToJByteArray(env, ckpState, ckStateLength);
    else
	jState = NULL_PTR;

    free(ckpState);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
    return jState;
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_SetOperationState
 * Signature: (J[BJJ)V
 * Parametermapping:                        *PKCS11*
 * @param   jlong jSessionHandle            CK_SESSION_HANDLE hSession
 * @param   jbyteArray jOperationState      CK_BYTE_PTR pOperationState
 *                                          CK_ULONG ulOperationStateLen
 * @param   jlong jEncryptionKeyHandle      CK_OBJECT_HANDLE hEncryptionKey
 * @param   jlong jAuthenticationKeyHandle  CK_OBJECT_HANDLE hAuthenticationKey
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1SetOperationState
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jbyteArray jOperationState, jlong jEncryptionKeyHandle,
     jlong jAuthenticationKeyHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_BYTE_PTR ckpState = NULL_PTR;
    CK_ULONG ckStateLength;
    CK_OBJECT_HANDLE ckEncryptionKeyHandle;
    CK_OBJECT_HANDLE ckAuthenticationKeyHandle;
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
    if (jByteArrayToCKByteArray(env, jOperationState, &ckpState, &ckStateLength)) {
	return;
    }
    ckEncryptionKeyHandle = jLongToCKULong(jEncryptionKeyHandle);
    ckAuthenticationKeyHandle = jLongToCKULong(jAuthenticationKeyHandle);

    rv = (*ckpFunctions->C_SetOperationState) (ckSessionHandle, ckpState, ckStateLength, ckEncryptionKeyHandle,
					       ckAuthenticationKeyHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    free(ckpState);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_Login
 * Signature: (JJ[CZ)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 * @param   jlong jUserType             CK_USER_TYPE userType
 * @param   jcharArray jPin             CK_CHAR_PTR pPin
 *                                      CK_ULONG ulPinLen
 * @param	jboolean jUseUtf8		if new Pin shall be saved as UTF8 encoding
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1Login
    (JNIEnv * env, jobject obj, jlong jSessionHandle, jlong jUserType, jcharArray jPin, jboolean jUseUtf8) {
    CK_SESSION_HANDLE ckSessionHandle;
    CK_USER_TYPE ckUserType;
    CK_CHAR_PTR ckpPinArray = NULL_PTR;
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
    ckUserType = jLongToCKULong(jUserType);

    ckUseUtf8 = jBooleanToCKBBool(jUseUtf8);
    if (ckUseUtf8 == TRUE) {
	if (jCharArrayToCKUTF8CharArray(env, jPin, &ckpPinArray, &ckPinLength)) {
	    return;
	}
    } else {
	if (jCharArrayToCKCharArray(env, jPin, &ckpPinArray, &ckPinLength)) {
	    return;
	}
    }

    rv = (*ckpFunctions->C_Login) (ckSessionHandle, ckUserType, ckpPinArray, ckPinLength);

    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    free(ckpPinArray);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    C_Logout
 * Signature: (J)V
 * Parametermapping:                    *PKCS11*
 * @param   jlong jSessionHandle        CK_SESSION_HANDLE hSession
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_C_1Logout
    (JNIEnv * env, jobject obj, jlong jSessionHandle) {
    CK_SESSION_HANDLE ckSessionHandle;
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

    rv = (*ckpFunctions->C_Logout) (ckSessionHandle);
    ckAssertReturnValueOK(env, rv, __FUNCTION__);

    TRACE0(tag_call, __FUNCTION__, "exiting ");
}
