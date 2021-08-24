/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.OrientManager;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * The server status end-point.
 */
@RestController
@RequestMapping("/sdm-ws-rest")
@Api(value = "Server status REST API")
public class ServerStatusController {
	private static final String OPEN_STATUS = "OPEN";

	private static final String CLOSED_STATUS = "CLOSED";

	@Autowired
	private OrientManager manager;

	/**
	 * Returns the server status. Possible states are: OPEN, CLOSED.
	 */
	@GetMapping(value = "/status", produces = "application/json")
	@ResponseStatus(value = HttpStatus.OK)
	@ApiOperation(value = "Health Check Service", response = String.class, notes = "Service to validate application is up & running.")
	public String getStatus() {
		String status = manager.isActive() ? OPEN_STATUS : CLOSED_STATUS;
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add(JsonConstants.STATUS, status);
		return builder.build().toString();
	}

	/**
	 * Closes the database.
	 */
	@PostMapping(value = "/close", produces = "application/json")
	@ResponseStatus(value = HttpStatus.OK)
	@ApiOperation(value = "Close Database", response = String.class, notes = "Service to close the database.")
	public String closeDatabase() {
		manager.shutdown();
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add(JsonConstants.STATUS, CLOSED_STATUS);

		return builder.build().toString();
	}
}
