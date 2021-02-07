package com.greydev.notionbackup;

import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static org.mockito.Mockito.*;


@Slf4j
public class NotionBackupTest {

	public void stuff() {
		// mock creation
		List mockedList = mock(List.class);

		// using mock object - it does not throw any "unexpected interaction" exception
		mockedList.add("one");
		mockedList.clear();

		// selective, explicit, highly readable verification
		verify(mockedList).add("one");
		verify(mockedList).clear();

		//

		// you can mock concrete classes, not only interfaces
		LinkedList mockedLinkedList = mock(LinkedList.class);

		// stubbing appears before the actual execution
		when(mockedLinkedList.get(0)).thenReturn("first");

		// the following prints "first"
		System.out.println(mockedLinkedList.get(0));

		// the following prints "null" because get(999) was not stubbed
		System.out.println(mockedLinkedList.get(999));
	}

}