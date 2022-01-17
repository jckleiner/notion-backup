package com.greydev.notionbackup;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;

import lombok.Data;
import lombok.NoArgsConstructor;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Result {

	private String state;

	private Status status;

	public boolean isSuccess() {
		return StringUtils.equalsIgnoreCase(this.state, "success");
	}

}


