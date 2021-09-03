/*
 * Copyright (c) 2002 Graz University of Technology. All rights reserved. Redistribution and use in source and binary
 * forms, with or without modification, are permitted provided that the following conditions are met: 1.
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the distribution. 3. The
 * end-user documentation included with the redistribution, if any, must include the following acknowledgment: "This
 * product includes software developed by IAIK of Graz University of Technology." Alternately, this acknowledgment may 
 * appear in the software itself, if and wherever such third-party acknowledgments normally appear. 4. The names "Graz 
 * University of Technology" and "IAIK of Graz University of Technology" must not be used to endorse or promote
 * products derived from this software without prior written permission. 5. Products derived from this software may
 * not be called "IAIK PKCS Wrapper", nor may "IAIK" appear in their name, without prior written permission of Graz
 * University of Technology.  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE LICENSOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */

#include "pkcs11t.h"

#ifndef COMMON_INCLUDE_GETATTRIBUTEVALUE_H_
#define COMMON_INCLUDE_GETATTRIBUTEVALUE_H_

#define attributeLengthBadCase 2048L
#define attributeNumberBadCase 20L

/*
 * functions to free allocated memory
 */
void freeAttributeArray(CK_ATTRIBUTE_PTR * ckpAttributes, CK_ULONG length, CK_BBOOL freeInnerArray);
void freeAttributeValue(CK_ATTRIBUTE_PTR ckpAttributes, CK_ULONG index, CK_BBOOL freeInnerArray);

/*
 * Compare returned pointer with expected correct pointer to recognize if array attributes are not supported 
 */
void checkArrayAttributePointers(CK_ATTRIBUTE_PTR currentAttribute, CK_ATTRIBUTE_PTR attributeCopy);

/*
 * Searches for attributes marked with length -1 and adds them as empty attribute to errAttributes. 
 */
void findAndFreeErroneousAttributes(CK_ATTRIBUTE_PTR ckpAttributes, CK_ULONG ckpAttributesLength,
				    CK_ATTRIBUTE_PTR * errAttributes, CK_ULONG * errAttributesLength,
				    CK_ULONG_PTR * errIndizes);

/*
 * Get attribute values by first determining the length, then allocating memory for the exact length and finally
 * getting the values. 
 */
int getAttributeValuesStd(JNIEnv * env, jobject obj, CK_RV * rv, CK_SESSION_HANDLE ckSessionHandle,
			  CK_OBJECT_HANDLE ckObjectHandle, CK_ATTRIBUTE_PTR ckpAttributes, CK_ULONG ckAttributesLength);

/*
 * function to allocate memory of assumed worst case number of bytes for an attribute array
 * without actually checking required number of bytes
 */
int preAllocateAttributeArrayValues(JNIEnv * env, const char *callerMethodName,
				    CK_ATTRIBUTE_PTR ckpAttributes, CK_ULONG ckAttributesLength,
				    CK_ATTRIBUTE_PTR arrayAttributes, CK_ULONG arrayAttributesLength);

/*
 * function returns (partly estimated) number of bytes required for the given attribute type 
 */
CK_ULONG getRequiredSpace(CK_ATTRIBUTE_TYPE type);

#endif				/* COMMON_INCLUDE_GETATTRIBUTEVALUE_H_ */
