package com.aggregator.core.http;

public interface HttpClientWrapper {
    String get(String url) throws HttpException;
}
