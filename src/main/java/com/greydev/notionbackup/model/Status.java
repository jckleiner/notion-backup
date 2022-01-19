package com.greydev.notionbackup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Status {

	private String type;

	private Integer pagesExported;

	@JsonProperty("exportURL")
	private String exportUrl;

}