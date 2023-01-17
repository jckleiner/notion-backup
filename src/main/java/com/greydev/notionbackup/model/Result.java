package com.greydev.notionbackup.model;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Result {

	private String state;

	private Status status;

	private String error; // error description

	public boolean isSuccess() {
		return StringUtils.equalsIgnoreCase(this.state, "success");
	}

	public boolean isFailure() {
		return StringUtils.equalsIgnoreCase(this.state, "failure");
	}

}


