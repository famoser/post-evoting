/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = GroupElementSerializer.class)
@JsonDeserialize(using = ZqElementDeserializer.class)
public interface ZqElementMixIn {
}
