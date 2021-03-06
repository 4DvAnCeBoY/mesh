package com.gentics.mesh.core.rest.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a basic login model.
 */
public class LoginRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Username of the user which should be logged in.")
	private String username;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Password of the user which should be logged in.")
	private String password;

	public LoginRequest() {
	}

	/**
	 * Return the password.
	 * 
	 * @return Password to be used for login
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Return the username.
	 * 
	 * @return Username to be used for login
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the login username.
	 * 
	 * @param username
	 *            Username to be used for login
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Set the login password.
	 * 
	 * @param password
	 *            Password to be used for login
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
