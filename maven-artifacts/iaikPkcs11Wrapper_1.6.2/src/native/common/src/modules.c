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
/* Functions for handling modules, mutex and callbacks                        */
/* ************************************************************************** */

/* The list of currently connected modules. Will normally contain one element,
 * but seldom more than a few.
 */
ModuleListNode *moduleListHead = NULL_PTR;
jobject moduleListLock = NULL_PTR;

/* The list of notify callback handles that are currently active and waiting
 * for callbacks from their sessions.
 */
#ifndef NO_CALLBACKS
NotifyListNode *notifyListHead = NULL_PTR;
jobject notifyListLock = NULL_PTR;
#endif				/* NO_CALLBACKS */

/* The initArgs that enable the application to do custom mutex-handling */
#ifndef NO_CALLBACKS
jobject jInitArgsObject = NULL_PTR;
CK_C_INITIALIZE_ARGS_PTR ckpGlobalInitArgs = NULL_PTR;
#endif				/* NO_CALLBACKS */

/* ************************************************************************** */
/* Functions for keeping track of currently active and loaded modules         */
/* ************************************************************************** */

/*
 * This method is used to do static initialization. This method is static and
 * synchronized. Summary: use this method like a static initialization block.
 *
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    initializeLibrary
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_initializeLibrary
    (JNIEnv * env, jclass thisClass) {
    TRACE0(tag_call, __FUNCTION__, "entering");
    if (moduleListLock == NULL_PTR) {
	moduleListLock = createLockObject(env);
    }
#ifndef NO_CALLBACKS
    if (notifyListLock == NULL_PTR) {
	notifyListLock = createLockObject(env);
    }
#endif
    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/* This method is designed to do a clean-up. It releases all global resources
 * of this library. By now, this function is not called. Calling from
 * JNI_OnUnload would be an option, but some VMs do not support JNI_OnUnload.
 *
 * Class:     iaik_pkcs_pkcs11_wrapper_PKCS11Implementation
 * Method:    finalizeLibrary
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_iaik_pkcs_pkcs11_wrapper_PKCS11Implementation_finalizeLibrary
    (JNIEnv * env, jclass thisClass) {
    /* remove all left lists and release the resources and the lock
     * objects that synchronize access to these lists.
     */
    removeAllModuleEntries(env);
    if (moduleListHead == NULL_PTR) {	/* check, if we removed the last active module */
	/* remove also the moduleListLock, it is no longer used */
	if (moduleListLock != NULL_PTR) {
	    destroyLockObject(env, moduleListLock);
	    moduleListLock = NULL_PTR;
	}
#ifndef NO_CALLBACKS
	/* remove all left notify callback entries */
	while (removeFirstNotifyEntry(env)) ;
	/* remove also the notifyListLock, it is no longer used */
	if (notifyListLock != NULL_PTR) {
	    destroyLockObject(env, notifyListLock);
	    notifyListLock = NULL_PTR;
	}
	if (jInitArgsObject != NULL_PTR) {
	    (*env)->DeleteGlobalRef(env, jInitArgsObject);
	}
	if (ckpGlobalInitArgs != NULL_PTR) {
	    if (ckpGlobalInitArgs->pReserved != NULL_PTR) {
		free(ckpGlobalInitArgs->pReserved);
	    }
	    free(ckpGlobalInitArgs);
	}
#endif				/* NO_CALLBACKS */
    }
    TRACE0(tag_call, __FUNCTION__, "exiting ");
}

/*
 * Add the given pkcs11Implementation object to the list of present modules.
 * Attach the given data to the entry. If the given pkcs11Implementation is
 * already in the list, just override its old module data with the new one.
 * None of the arguments can be NULL_PTR. If one of the arguments is NULL_PTR, this
 * function does nothing.
 */
void putModuleEntry(JNIEnv * env, jobject pkcs11Implementation, ModuleData * moduleData)
{
    ModuleListNode *currentNode, *newNode;

    if (pkcs11Implementation == NULL_PTR) {
	return;
    }
    if (moduleData == NULL_PTR) {
	return;
    }

    (*env)->MonitorEnter(env, moduleListLock);	/* synchronize access to list */

    if (moduleListHead == NULL_PTR) {
	/* this is the first entry */
	newNode = (ModuleListNode *) malloc(sizeof(ModuleListNode));
	if (newNode == NULL_PTR) {
	    throwOutOfMemoryError(env);
	    return;
	}
	newNode->pkcs11Implementation = pkcs11Implementation;
	newNode->moduleData = moduleData;
	newNode->next = NULL_PTR;

	moduleListHead = newNode;
    } else {
	/* go to the last entry; i.e. the first node which's 'next' is NULL_PTR.
	 * we also stop, when we the pkcs11Implementation object is already in the list.
	 * then we override the old moduleData with the new one
	 */
	currentNode = moduleListHead;
	while ((currentNode->next != NULL_PTR)
	       && (!equals(env, pkcs11Implementation, currentNode->pkcs11Implementation))) {
	    currentNode = currentNode->next;
	}
	if (!equals(env, pkcs11Implementation, currentNode->pkcs11Implementation)) {
	    /* this pkcs11Implementation is not present yet, append the new node */
	    newNode = (ModuleListNode *) malloc(sizeof(ModuleListNode));
	    if (newNode == NULL_PTR) {
		throwOutOfMemoryError(env);
		return;
	    }
	    newNode->pkcs11Implementation = pkcs11Implementation;
	    newNode->moduleData = moduleData;
	    newNode->next = NULL_PTR;

	    currentNode->next = newNode;
	} else {
	    /* this pkcs11Implementation is already present, set the new moduleData */
	    currentNode->moduleData = moduleData;
	}
    }

    (*env)->MonitorExit(env, moduleListLock);	/* synchronize access to list */
}

/*
 * Get the module data of the entry for the given pkcs11Implementation. Returns
 * NULL_PTR, if the pkcs11Implementation is not in the list.
 */
ModuleData *getModuleEntry(JNIEnv * env, jobject pkcs11Implementation)
{
    ModuleListNode *currentNode;
    ModuleData *moduleDataOfFoundNode;

    moduleDataOfFoundNode = NULL_PTR;

    if (pkcs11Implementation == NULL_PTR) {
	/* Nothing to do. */
	return NULL_PTR;
    }

    /* We stop, when we the pkcs11Implementation object is already in the list.
     * We also stop, when we reach the end; i.e. the first node which's 'next'
     * is NULL_PTR.
     */
    (*env)->MonitorEnter(env, moduleListLock);	/* synchronize access to list */

    if (moduleListHead != NULL_PTR) {
	currentNode = moduleListHead;
	while ((currentNode->next != NULL_PTR)
	       && (!equals(env, pkcs11Implementation, currentNode->pkcs11Implementation))) {
	    currentNode = currentNode->next;
	}
	if (equals(env, pkcs11Implementation, currentNode->pkcs11Implementation)) {
	    /* we found the entry */
	    moduleDataOfFoundNode = currentNode->moduleData;
	} else {
	    /* the entry is not in the list */
	    moduleDataOfFoundNode = NULL_PTR;
	}
    }

    (*env)->MonitorExit(env, moduleListLock);	/* synchronize access to list */

    return moduleDataOfFoundNode;
}

/*
 * Removes the entry for the given pkcs11Implementation from the list. Returns
 * the module's data, after the node was removed. If this function returns NULL_PTR
 * the pkcs11Implementation was not in the list.
 */
ModuleData *removeModuleEntry(JNIEnv * env, jobject pkcs11Implementation)
{
    ModuleListNode *currentNode, *previousNode;
    ModuleData *moduleDataOfFoundNode;

    moduleDataOfFoundNode = NULL_PTR;

    if (pkcs11Implementation == NULL_PTR) {
	/* Nothing to do. */
	return NULL_PTR;
    }

    /* We stop, when we the pkcs11Implementation object is already in the list.
     * We also stop, when we reach the end; i.e. the first node which's 'next'
     * is NULL_PTR. We remember the previous node the be able to remove the node
     * later.
     */
    (*env)->MonitorEnter(env, moduleListLock);	/* synchronize access to list */

    if (moduleListHead != NULL_PTR) {
	currentNode = moduleListHead;
	previousNode = NULL_PTR;
	while ((currentNode->next != NULL_PTR)
	       && (!equals(env, pkcs11Implementation, currentNode->pkcs11Implementation))) {
	    previousNode = currentNode;
	    currentNode = currentNode->next;
	}
	if (equals(env, pkcs11Implementation, currentNode->pkcs11Implementation)) {
	    /* we found the entry, so remove it */
	    if (previousNode == NULL_PTR) {
		/* it's the first node */
		moduleListHead = currentNode->next;
	    } else {
		previousNode->next = currentNode->next;
	    }
	    moduleDataOfFoundNode = currentNode->moduleData;
	    (*env)->DeleteGlobalRef(env, currentNode->pkcs11Implementation);
	    free(currentNode);
	} else {
	    /* the entry is not in the list */
	    moduleDataOfFoundNode = NULL_PTR;
	}
    }

    (*env)->MonitorExit(env, moduleListLock);	/* synchronize access to list */

    return moduleDataOfFoundNode;
}

/*
 * Removes all present entries from the list of modules and frees all
 * associated resources. This function is used for clean-up.
 */
void removeAllModuleEntries(JNIEnv * env)
{
    ModuleListNode *currentNode, *nextNode;

    (*env)->MonitorEnter(env, moduleListLock);	/* synchronize access to list */

    currentNode = moduleListHead;
    while (currentNode != NULL_PTR) {
	nextNode = currentNode->next;
	(*env)->DeleteGlobalRef(env, currentNode->pkcs11Implementation);
	free(currentNode);
	currentNode = nextNode;
    }
    moduleListHead = NULL_PTR;

    (*env)->MonitorExit(env, moduleListLock);	/* synchronize access to list */
}

/*
 * Create a new object for locking.
 */
jobject createLockObject(JNIEnv * env)
{
    jclass jObjectClass;
    jobject jLockObject;
    jmethodID jConstructor;

    jObjectClass = (*env)->FindClass(env, "java/lang/Object");
    assert(jObjectClass != 0);
    jConstructor = (*env)->GetMethodID(env, jObjectClass, "<init>", "()V");
    assert(jConstructor != 0);
    jLockObject = (*env)->NewObject(env, jObjectClass, jConstructor);
    assert(jLockObject != 0);
    jLockObject = (*env)->NewGlobalRef(env, jLockObject);

    return jLockObject;
}

/*
 * Create a new object for locking.
 */
void destroyLockObject(JNIEnv * env, jobject jLockObject)
{
    if (jLockObject != NULL_PTR) {
	(*env)->DeleteGlobalRef(env, jLockObject);
    }
}

/*
 * Returns 1, if the given pkcs11Implementation is in the list.
 * 0, otherwise.
 */
int isModulePresent(JNIEnv * env, jobject pkcs11Implementation)
{
    int present;

    ModuleData *moduleData = getModuleEntry(env, pkcs11Implementation);

    present = (moduleData != NULL_PTR) ? 1 : 0;

    return present;
}

/* ************************************************************************** */
/* Functions for mutex handling and notification callbacks       */
/* ************************************************************************** */

/*
 * converts the InitArgs object to a CK_C_INITIALIZE_ARGS structure and sets the functions
 * that will call the right Java mutex functions
 *
 * @param env - used to call JNI functions to get the Java classes, objects, methods and fields
 * @param pInitArgs - the InitArgs object with the Java mutex functions to call
 * @return - the pointer to the CK_C_INITIALIZE_ARGS structure with the functions that will call
 *           the corresponding Java functions
 */
CK_C_INITIALIZE_ARGS_PTR makeCKInitArgsAdapter(JNIEnv * env, jobject jInitArgs, jboolean jUseUtf8)
{
    CK_C_INITIALIZE_ARGS_PTR ckpInitArgs;
    jclass jInitArgsClass = (*env)->FindClass(env, CLASS_C_INITIALIZE_ARGS);
    jfieldID fieldID;
    jlong jFlags;
    jobject jReserved;
    CK_ULONG ckReservedLength;
#ifndef NO_CALLBACKS
    jobject jMutexHandler;
#endif				/* NO_CALLBACKS */

    if (jInitArgs == NULL_PTR) {
	return NULL_PTR;
    }

    /* convert the Java InitArgs object to a pointer to a CK_C_INITIALIZE_ARGS structure */
    ckpInitArgs = (CK_C_INITIALIZE_ARGS_PTR) malloc(sizeof(CK_C_INITIALIZE_ARGS));
    if (ckpInitArgs == NULL_PTR) {
	throwOutOfMemoryError(env);
	return NULL_PTR;
    }

    /* Set the mutex functions that will call the Java mutex functions, but
     * only set it, if the field is not NULL_PTR.
     */
#ifdef NO_CALLBACKS
    ckpInitArgs->CreateMutex = NULL_PTR;
    ckpInitArgs->DestroyMutex = NULL_PTR;
    ckpInitArgs->LockMutex = NULL_PTR;
    ckpInitArgs->UnlockMutex = NULL_PTR;
#else
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "CreateMutex", CLASS_NAME(CLASS_CREATEMUTEX));
    assert(fieldID != 0);
    jMutexHandler = (*env)->GetObjectField(env, jInitArgs, fieldID);
    ckpInitArgs->CreateMutex = (jMutexHandler != NULL_PTR) ? &callJCreateMutex : NULL_PTR;

    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "DestroyMutex", CLASS_NAME(CLASS_DESTROYMUTEX));
    assert(fieldID != 0);
    jMutexHandler = (*env)->GetObjectField(env, jInitArgs, fieldID);
    ckpInitArgs->DestroyMutex = (jMutexHandler != NULL_PTR) ? &callJDestroyMutex : NULL_PTR;

    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "LockMutex", CLASS_NAME(CLASS_LOCKMUTEX));
    assert(fieldID != 0);
    jMutexHandler = (*env)->GetObjectField(env, jInitArgs, fieldID);
    ckpInitArgs->LockMutex = (jMutexHandler != NULL_PTR) ? &callJLockMutex : NULL_PTR;

    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "UnlockMutex", CLASS_NAME(CLASS_UNLOCKMUTEX));
    assert(fieldID != 0);
    jMutexHandler = (*env)->GetObjectField(env, jInitArgs, fieldID);
    ckpInitArgs->UnlockMutex = (jMutexHandler != NULL_PTR) ? &callJUnlockMutex : NULL_PTR;

    if ((ckpInitArgs->CreateMutex != NULL_PTR)
	|| (ckpInitArgs->DestroyMutex != NULL_PTR)
	|| (ckpInitArgs->LockMutex != NULL_PTR)
	|| (ckpInitArgs->UnlockMutex != NULL_PTR)) {
	/* we only need to keep a global copy, if we need callbacks */
	/* set the global object jInitArgs so that the right Java mutex functions will be called */
	jInitArgsObject = (*env)->NewGlobalRef(env, jInitArgs);
	ckpGlobalInitArgs = (CK_C_INITIALIZE_ARGS_PTR) malloc(sizeof(CK_C_INITIALIZE_ARGS));
	if (ckpGlobalInitArgs == NULL_PTR) {
	    free(ckpInitArgs);
	    throwOutOfMemoryError(env);
	    return NULL_PTR;
	}
	memcpy(ckpGlobalInitArgs, ckpInitArgs, sizeof(CK_C_INITIALIZE_ARGS));
    }
#endif				/* NO_CALLBACKS */

    /* convert and set the flags field */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "flags", "J");
    assert(fieldID != 0);
    jFlags = (*env)->GetLongField(env, jInitArgs, fieldID);
    ckpInitArgs->flags = jLongToCKULong(jFlags);

    /* pReserved should be NULL_PTR in this version */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "pReserved", "Ljava/lang/Object;");
    assert(fieldID != 0);
    jReserved = (*env)->GetObjectField(env, jInitArgs, fieldID);

    /* we try to convert the reserved parameter also */
    jObjectToPrimitiveCKObjectPtrPtr(env, jReserved, &(ckpInitArgs->pReserved), &ckReservedLength, jUseUtf8);

    return ckpInitArgs;
}

#ifndef NO_CALLBACKS

/*
 * is the function that gets called by PKCS#11 to create a mutex and calls the Java
 * CreateMutex function
 *
 * @param env - used to call JNI functions to get the Java classes, objects, methods and fields
 * @param ppMutex - the new created mutex
 * @return - should return CKR_OK if the mutex creation was ok
 */
CK_RV callJCreateMutex(CK_VOID_PTR_PTR ppMutex)
{
    JavaVM *jvm;
    JNIEnv *env;
    jsize actualNumberVMs;
    jint returnValue;
    jthrowable pkcs11Exception;
    jclass pkcs11ExceptionClass;
    jlong errorCode;
    CK_RV rv = CKR_OK;
    int wasAttached = 1;
    jclass jCreateMutexClass;
    jclass jInitArgsClass;
    jmethodID methodID;
    jfieldID fieldID;
    jobject jCreateMutex;
    jobject jMutex;

    /* Get the currently running Java VM */
    returnValue = JNI_GetCreatedJavaVMs(&jvm, (jsize) 1, &actualNumberVMs);
    if ((returnValue != 0) || (actualNumberVMs <= 0)) {
	return rv;
    }				/* there is no VM running */

    /* Determine, if current thread is already attached */
    returnValue = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2);
    if (returnValue == JNI_EDETACHED) {
	/* thread detached, so attach it */
	wasAttached = 0;
	returnValue = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL_PTR);
    } else if (returnValue == JNI_EVERSION) {
	/* this version of JNI is not supported, so just try to attach */
	/* we assume it was attached to ensure that this thread is not detached
	 * afterwards even though it should not
	 */
	wasAttached = 1;
	returnValue = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL_PTR);
    } else {
	/* attached */
	wasAttached = 1;
    }

    jCreateMutexClass = (*env)->FindClass(env, CLASS_CREATEMUTEX);
    jInitArgsClass = (*env)->FindClass(env, CLASS_C_INITIALIZE_ARGS);

    /* get the CreateMutex object out of the jInitArgs object */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "CreateMutex", CLASS_NAME(CLASS_CREATEMUTEX));
    assert(fieldID != 0);
    jCreateMutex = (*env)->GetObjectField(env, jInitArgsObject, fieldID);
    assert(jCreateMutex != 0);

    /* call the CK_CREATEMUTEX function of the CreateMutex object */
    /* and get the new Java mutex object */
    methodID = (*env)->GetMethodID(env, jCreateMutexClass, "CK_CREATEMUTEX", "()Ljava/lang/Object;");
    assert(methodID != 0);
    jMutex = (*env)->CallObjectMethod(env, jCreateMutex, methodID);

    /* set a global reference on the Java mutex */
    jMutex = (*env)->NewGlobalRef(env, jMutex);
    /* convert the Java mutex to a CK mutex */
    *ppMutex = jObjectToCKVoidPtr(jMutex);

    /* check, if callback threw an exception */
    pkcs11Exception = (*env)->ExceptionOccurred(env);

    if (pkcs11Exception != NULL_PTR) {
	/* The was an exception thrown, now we get the error-code from it */
	pkcs11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
	methodID = (*env)->GetMethodID(env, pkcs11ExceptionClass, "getErrorCode", "()J");
	assert(methodID != 0);
	errorCode = (*env)->CallLongMethod(env, pkcs11Exception, methodID);
	rv = jLongToCKULong(errorCode);
    }

    /* if we attached this thread to the VM just for callback, we detach it now */
    if (wasAttached) {
	returnValue = (*jvm)->DetachCurrentThread(jvm);
    }

    return rv;
}

/*
 * is the function that gets called by PKCS#11 to destroy a mutex and calls the Java
 * DestroyMutex function
 *
 * @param env - used to call JNI functions to get the Java classes, objects, methods and fields
 * @param pMutex - the mutex to destroy
 * @return - should return CKR_OK if the mutex was destroyed
 */
CK_RV callJDestroyMutex(CK_VOID_PTR pMutex)
{
    JavaVM *jvm;
    JNIEnv *env;
    jsize actualNumberVMs;
    jint returnValue;
    jthrowable pkcs11Exception;
    jclass pkcs11ExceptionClass;
    jlong errorCode;
    CK_RV rv = CKR_OK;
    int wasAttached = 1;
    jclass jDestroyMutexClass;
    jclass jInitArgsClass;
    jmethodID methodID;
    jfieldID fieldID;
    jobject jDestroyMutex;
    jobject jMutex;

    /* Get the currently running Java VM */
    returnValue = JNI_GetCreatedJavaVMs(&jvm, (jsize) 1, &actualNumberVMs);
    if ((returnValue != 0) || (actualNumberVMs <= 0)) {
	return rv;
    }				/* there is no VM running */

    /* Determine, if current thread is already attached */
    returnValue = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2);
    if (returnValue == JNI_EDETACHED) {
	/* thread detached, so attach it */
	wasAttached = 0;
	returnValue = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL_PTR);
    } else if (returnValue == JNI_EVERSION) {
	/* this version of JNI is not supported, so just try to attach */
	/* we assume it was attached to ensure that this thread is not detached
	 * afterwards even though it should not
	 */
	wasAttached = 1;
	returnValue = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL_PTR);
    } else {
	/* attached */
	wasAttached = 1;
    }

    jDestroyMutexClass = (*env)->FindClass(env, CLASS_DESTROYMUTEX);
    jInitArgsClass = (*env)->FindClass(env, CLASS_C_INITIALIZE_ARGS);

    /* convert the CK mutex to a Java mutex */
    jMutex = ckVoidPtrToJObject(pMutex);

    /* get the DestroyMutex object out of the jInitArgs object */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "DestroyMutex", CLASS_NAME(CLASS_DESTROYMUTEX));
    assert(fieldID != 0);
    jDestroyMutex = (*env)->GetObjectField(env, jInitArgsObject, fieldID);
    assert(jDestroyMutex != 0);

    /* call the CK_DESTROYMUTEX method of the DestroyMutex object */
    methodID = (*env)->GetMethodID(env, jDestroyMutexClass, "CK_DESTROYMUTEX", "(Ljava/lang/Object;)V");
    assert(methodID != 0);
    (*env)->CallVoidMethod(env, jDestroyMutex, methodID, jMutex);

    /* delete the global reference on the Java mutex */
    (*env)->DeleteGlobalRef(env, jMutex);

    /* check, if callback threw an exception */
    pkcs11Exception = (*env)->ExceptionOccurred(env);

    if (pkcs11Exception != NULL_PTR) {
	/* The was an exception thrown, now we get the error-code from it */
	pkcs11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
	methodID = (*env)->GetMethodID(env, pkcs11ExceptionClass, "getErrorCode", "()J");
	assert(methodID != 0);
	errorCode = (*env)->CallLongMethod(env, pkcs11Exception, methodID);
	rv = jLongToCKULong(errorCode);
    }

    /* if we attached this thread to the VM just for callback, we detach it now */
    if (wasAttached) {
	returnValue = (*jvm)->DetachCurrentThread(jvm);
    }

    return rv;
}

/*
 * is the function that gets called by PKCS#11 to lock a mutex and calls the Java
 * LockMutex function
 *
 * @param env - used to call JNI functions to get the Java classes, objects, methods and fields
 * @param pMutex - the mutex to lock
 * @return - should return CKR_OK if the mutex was not locked already
 */
CK_RV callJLockMutex(CK_VOID_PTR pMutex)
{
    JavaVM *jvm;
    JNIEnv *env;
    jsize actualNumberVMs;
    jint returnValue;
    jthrowable pkcs11Exception;
    jclass pkcs11ExceptionClass;
    jlong errorCode;
    CK_RV rv = CKR_OK;
    int wasAttached = 1;
    jclass jLockMutexClass;
    jclass jInitArgsClass;
    jmethodID methodID;
    jfieldID fieldID;
    jobject jLockMutex;
    jobject jMutex;

    /* Get the currently running Java VM */
    returnValue = JNI_GetCreatedJavaVMs(&jvm, (jsize) 1, &actualNumberVMs);
    if ((returnValue != 0) || (actualNumberVMs <= 0)) {
	return rv;
    }				/* there is no VM running */

    /* Determine, if current thread is already attached */
    returnValue = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2);
    if (returnValue == JNI_EDETACHED) {
	/* thread detached, so attach it */
	wasAttached = 0;
	returnValue = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL_PTR);
    } else if (returnValue == JNI_EVERSION) {
	/* this version of JNI is not supported, so just try to attach */
	/* we assume it was attached to ensure that this thread is not detached
	 * afterwards even though it should not
	 */
	wasAttached = 1;
	returnValue = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL_PTR);
    } else {
	/* attached */
	wasAttached = 1;
    }

    jLockMutexClass = (*env)->FindClass(env, CLASS_LOCKMUTEX);
    jInitArgsClass = (*env)->FindClass(env, CLASS_C_INITIALIZE_ARGS);

    /* convert the CK mutex to a Java mutex */
    jMutex = ckVoidPtrToJObject(pMutex);

    /* get the LockMutex object out of the jInitArgs object */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "LockMutex", CLASS_NAME(CLASS_LOCKMUTEX));
    assert(fieldID != 0);
    jLockMutex = (*env)->GetObjectField(env, jInitArgsObject, fieldID);
    assert(jLockMutex != 0);

    /* call the CK_LOCKMUTEX method of the LockMutex object */
    methodID = (*env)->GetMethodID(env, jLockMutexClass, "CK_LOCKMUTEX", "(Ljava/lang/Object;)V");
    assert(methodID != 0);
    (*env)->CallVoidMethod(env, jLockMutex, methodID, jMutex);

    /* check, if callback threw an exception */
    pkcs11Exception = (*env)->ExceptionOccurred(env);

    if (pkcs11Exception != NULL_PTR) {
	/* The was an exception thrown, now we get the error-code from it */
	pkcs11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
	methodID = (*env)->GetMethodID(env, pkcs11ExceptionClass, "getErrorCode", "()J");
	assert(methodID != 0);
	errorCode = (*env)->CallLongMethod(env, pkcs11Exception, methodID);
	rv = jLongToCKULong(errorCode);
    }

    /* if we attached this thread to the VM just for callback, we detach it now */
    if (wasAttached) {
	returnValue = (*jvm)->DetachCurrentThread(jvm);
    }

    return rv;
}

/*
 * is the function that gets called by PKCS#11 to unlock a mutex and calls the Java
 * UnlockMutex function
 *
 * @param env - used to call JNI functions to get the Java classes, objects, methods and fields
 * @param pMutex - the mutex to unlock
 * @return - should return CKR_OK if the mutex was not unlocked already
 */
CK_RV callJUnlockMutex(CK_VOID_PTR pMutex)
{
    JavaVM *jvm;
    JNIEnv *env;
    jsize actualNumberVMs;
    jint returnValue;
    jthrowable pkcs11Exception;
    jclass pkcs11ExceptionClass;
    jlong errorCode;
    CK_RV rv = CKR_OK;
    int wasAttached = 1;
    jclass jUnlockMutexClass;
    jclass jInitArgsClass;
    jmethodID methodID;
    jfieldID fieldID;
    jobject jUnlockMutex;
    jobject jMutex;

    /* Get the currently running Java VM */
    returnValue = JNI_GetCreatedJavaVMs(&jvm, (jsize) 1, &actualNumberVMs);
    if ((returnValue != 0) || (actualNumberVMs <= 0)) {
	return rv;
    }				/* there is no VM running */

    /* Determine, if current thread is already attached */
    returnValue = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2);
    if (returnValue == JNI_EDETACHED) {
	/* thread detached, so attach it */
	wasAttached = 0;
	returnValue = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL_PTR);
    } else if (returnValue == JNI_EVERSION) {
	/* this version of JNI is not supported, so just try to attach */
	/* we assume it was attached to ensure that this thread is not detached
	 * afterwards even though it should not
	 */
	wasAttached = 1;
	returnValue = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL_PTR);
    } else {
	/* attached */
	wasAttached = 1;
    }

    jUnlockMutexClass = (*env)->FindClass(env, CLASS_UNLOCKMUTEX);
    jInitArgsClass = (*env)->FindClass(env, CLASS_C_INITIALIZE_ARGS);

    /* convert the CK-type mutex to a Java mutex */
    jMutex = ckVoidPtrToJObject(pMutex);

    /* get the UnlockMutex object out of the jInitArgs object */
    fieldID = (*env)->GetFieldID(env, jInitArgsClass, "UnlockMutex", CLASS_NAME(CLASS_UNLOCKMUTEX));
    assert(fieldID != 0);
    jUnlockMutex = (*env)->GetObjectField(env, jInitArgsObject, fieldID);
    assert(jUnlockMutex != 0);

    /* call the CK_UNLOCKMUTEX method of the UnLockMutex object */
    methodID = (*env)->GetMethodID(env, jUnlockMutexClass, "CK_UNLOCKMUTEX", "(Ljava/lang/Object;)V");
    assert(methodID != 0);
    (*env)->CallVoidMethod(env, jUnlockMutex, methodID, jMutex);

    /* check, if callback threw an exception */
    pkcs11Exception = (*env)->ExceptionOccurred(env);

    if (pkcs11Exception != NULL_PTR) {
	/* The was an exception thrown, now we get the error-code from it */
	pkcs11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
	methodID = (*env)->GetMethodID(env, pkcs11ExceptionClass, "getErrorCode", "()J");
	assert(methodID != 0);
	errorCode = (*env)->CallLongMethod(env, pkcs11Exception, methodID);
	rv = jLongToCKULong(errorCode);
    }

    /* if we attached this thread to the VM just for callback, we detach it now */
    if (wasAttached) {
	returnValue = (*jvm)->DetachCurrentThread(jvm);
    }

    return rv;
}

/* ************************************************************************** */
/* Functions for keeping track of notify callbacks                            */
/* ************************************************************************** */

#ifndef NO_CALLBACKS

/*
 * Add the given notify encapsulation object to the list of active notify
 * objects.
 * If notifyEncapsulation is NULL_PTR, this function does nothing.
 */
void putNotifyEntry(JNIEnv * env, CK_SESSION_HANDLE hSession, NotifyEncapsulation * notifyEncapsulation)
{
    NotifyListNode *currentNode, *newNode;

    if (notifyEncapsulation == NULL_PTR) {
	return;
    }

    newNode = (NotifyListNode *) malloc(sizeof(NotifyListNode));
    if (newNode == NULL_PTR) {
	throwOutOfMemoryError(env);
	return;
    }
    newNode->hSession = hSession;
    newNode->notifyEncapsulation = notifyEncapsulation;
    newNode->next = NULL_PTR;

    (*env)->MonitorEnter(env, notifyListLock);	/* synchronize access to list */

    if (notifyListHead == NULL_PTR) {
	/* this is the first entry */
	notifyListHead = newNode;
    } else {
	/* go to the last entry; i.e. the first node which's 'next' is NULL_PTR.
	 */
	currentNode = notifyListHead;
	while (currentNode->next != NULL_PTR) {
	    currentNode = currentNode->next;
	}
	currentNode->next = newNode;
    }

    (*env)->MonitorExit(env, notifyListLock);	/* synchronize access to list */
}

/*
 * Removes the active notifyEncapsulation object used with the given session and
 * returns it. If there is no notifyEncapsulation active for this session, this
 * function returns NULL_PTR.
 */
NotifyEncapsulation *removeNotifyEntry(JNIEnv * env, CK_SESSION_HANDLE hSession)
{
    NotifyEncapsulation *notifyEncapsulation;
    NotifyListNode *currentNode, *previousNode;

    (*env)->MonitorEnter(env, notifyListLock);	/* synchronize access to list */

    if (notifyListHead == NULL_PTR) {
	/* this is the first entry */
	notifyEncapsulation = NULL_PTR;
    } else {
	/* Find the node with the wanted session handle. Also stop, when we reach
	 * the last entry; i.e. the first node which's 'next' is NULL_PTR.
	 */
	currentNode = notifyListHead;
	previousNode = NULL_PTR;

	while ((currentNode->hSession != hSession) && (currentNode->next != NULL_PTR)) {
	    previousNode = currentNode;
	    currentNode = currentNode->next;
	}

	if (currentNode->hSession == hSession) {
	    /* We found a entry for the wanted session, now remove it. */
	    if (previousNode == NULL_PTR) {
		/* it's the first node */
		notifyListHead = currentNode->next;
	    } else {
		previousNode->next = currentNode->next;
	    }
	    notifyEncapsulation = currentNode->notifyEncapsulation;
	    free(currentNode);
	} else {
	    /* We did not find a entry for this session */
	    notifyEncapsulation = NULL_PTR;
	}
    }

    (*env)->MonitorExit(env, notifyListLock);	/* synchronize access to list */

    return notifyEncapsulation;
}

/*

 * Removes the first notifyEncapsulation object. If there is no notifyEncapsulation,
 * this function returns NULL_PTR.
 */
NotifyEncapsulation *removeFirstNotifyEntry(JNIEnv * env)
{
    NotifyEncapsulation *notifyEncapsulation;
    NotifyListNode *currentNode;

    (*env)->MonitorEnter(env, notifyListLock);	/* synchronize access to list */

    if (notifyListHead == NULL_PTR) {
	/* this is the first entry */
	notifyEncapsulation = NULL_PTR;
    } else {
	/* Remove the first entry. */
	currentNode = notifyListHead;
	notifyListHead = notifyListHead->next;
	notifyEncapsulation = currentNode->notifyEncapsulation;
	free(currentNode);
    }

    (*env)->MonitorExit(env, notifyListLock);	/* synchronize access to list */

    return notifyEncapsulation;
}

#endif				/* NO_CALLBACKS */

/*
 * The function handling notify callbacks. It casts the pApplication parameter
 * back to a NotifyEncapsulation structure and retrieves the Notify object and
 * the application data from it.
 *
 * @param hSession The session, this callback is coming from.
 * @param event The type of event that occurred.
 * @param pApplication The application data as passed in upon OpenSession. In
                       this wrapper we always pass in a NotifyEncapsulation
                       object, which holds necessary information for delegating
                       the callback to the Java VM.
 * @return
 */
CK_RV notifyCallback(CK_SESSION_HANDLE hSession,	/* the session's handle */
		     CK_NOTIFICATION event, CK_VOID_PTR pApplication	/* passed to C_OpenSession */
    )
{
    NotifyEncapsulation *notifyEncapsulation;
    JavaVM *jvm;
    JNIEnv *env;
    jsize actualNumberVMs;
    jint returnValue;
    jlong jSessionHandle;
    jlong jEvent;
    jclass ckNotifyClass;
    jmethodID jmethod;
    jthrowable pkcs11Exception;
    jclass pkcs11ExceptionClass;
    jlong errorCode;
    CK_RV rv = CKR_OK;
    int wasAttached = 1;

    if (pApplication == NULL_PTR) {
	return rv;
    }				/* This should not occur in this wrapper. */

    notifyEncapsulation = (NotifyEncapsulation *) pApplication;

    /* Get the currently running Java VM */
    returnValue = JNI_GetCreatedJavaVMs(&jvm, (jsize) 1, &actualNumberVMs);
    if ((returnValue != 0) || (actualNumberVMs <= 0)) {
	return rv;
    }				/* there is no VM running */

    /* Determine, if current thread is already attached */
    returnValue = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2);
    if (returnValue == JNI_EDETACHED) {
	/* thread detached, so attach it */
	wasAttached = 0;
	returnValue = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL_PTR);
    } else if (returnValue == JNI_EVERSION) {
	/* this version of JNI is not supported, so just try to attach */
	/* we assume it was attached to ensure that this thread is not detached
	 * afterwards even though it should not
	 */
	wasAttached = 1;
	returnValue = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL_PTR);
    } else {
	/* attached */
	wasAttached = 1;
    }

    jSessionHandle = ckULongToJLong(hSession);
    jEvent = ckULongToJLong(event);

    ckNotifyClass = (*env)->FindClass(env, CLASS_NOTIFY);
    assert(ckNotifyClass != 0);
    jmethod = (*env)->GetMethodID(env, ckNotifyClass, "CK_NOTIFY", "(JJLjava/lang/Object;)V");
    assert(jmethod != 0);
    (*env)->CallVoidMethod(env, notifyEncapsulation->jNotifyObject, jmethod,
			   jSessionHandle, jEvent, notifyEncapsulation->jApplicationData);

    /* check, if callback threw an exception */
    pkcs11Exception = (*env)->ExceptionOccurred(env);

    if (pkcs11Exception != NULL_PTR) {
	/* The was an exception thrown, now we get the error-code from it */
	pkcs11ExceptionClass = (*env)->FindClass(env, CLASS_PKCS11EXCEPTION);
	jmethod = (*env)->GetMethodID(env, pkcs11ExceptionClass, "getErrorCode", "()J");
	assert(jmethod != 0);
	errorCode = (*env)->CallLongMethod(env, pkcs11Exception, jmethod);
	rv = jLongToCKULong(errorCode);
    }

    /* if we attached this thread to the VM just for callback, we detach it now */
    if (wasAttached) {
	returnValue = (*jvm)->DetachCurrentThread(jvm);
    }

    return rv;
}

#endif				/* NO_CALLBACKS */
