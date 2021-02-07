package com.greydev.notionbackup;

/*
OkHttpClients should be shared

OkHttp performs best when you create a single OkHttpClient instance and reuse it for all of your HTTP calls.
This is because each client holds its own connection pool and thread pools. Reusing connections and threads reduces latency and saves memory.
Conversely, creating a client for each request wastes resources on idle pools.

Use new OkHttpClient() to create a shared instance with the default settings,
Or use new OkHttpClient.Builder() to create a shared instance with custom settings

Customize your client with newBuilder()
You can customize a shared OkHttpClient instance with newBuilder().
This builds a client that shares the same connection pool, thread pools, and configuration.
Use the builder methods to configure the derived client for a specific purpose.
This example shows a call with a short 500 millisecond timeout:

   OkHttpClient eagerClient = client.newBuilder()
       .readTimeout(500, TimeUnit.MILLISECONDS)
       .build();
   Response response = eagerClient.newCall(request).execute();

 */
public class OkHttpClientSingleton {
}
