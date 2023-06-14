/*
Copyright (C)

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
details.

You should have received a copy of the GNU Affero General Public License along with this program.
If not, see https://www.gnu.org/licenses/.
*/

package it.gov.pagopa.project.util;


import com.microsoft.azure.functions.*;

import java.util.Map;
import java.util.HashMap;

/**
 * The mock for HttpResponseMessage, can be used in unit tests to verify if the
 * returned response by HTTP trigger function is correct or not.
 */
public class HttpResponseMessageMock implements HttpResponseMessage {
    private int httpStatusCode;
    private HttpStatusType httpStatus;
    private Object body;
    private Map<String, String> headers;

    public HttpResponseMessageMock(HttpStatusType status, Map<String, String> headers, Object body) {
        this.httpStatus = status;
        this.httpStatusCode = status.value();
        this.headers = headers;
        this.body = body;
    }

    @Override
    public HttpStatusType getStatus() {
        return this.httpStatus;
    }

    @Override
    public int getStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String getHeader(String key) {
        return this.headers.get(key);
    }

    @Override
    public Object getBody() {
        return this.body;
    }

    public static class HttpResponseMessageBuilderMock implements HttpResponseMessage.Builder {
        private Object body;
        private int httpStatusCode;
        private Map<String, String> headers = new HashMap<>();
        private HttpStatusType httpStatus;

        public Builder status(HttpStatus status) {
            this.httpStatusCode = status.value();
            this.httpStatus = status;
            return this;
        }

        @Override
        public Builder status(HttpStatusType httpStatusType) {
            this.httpStatusCode = httpStatusType.value();
            this.httpStatus = httpStatusType;
            return this;
        }

        @Override
        public HttpResponseMessage.Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        @Override
        public HttpResponseMessage.Builder body(Object body) {
            this.body = body;
            return this;
        }

        @Override
        public HttpResponseMessage build() {
            return new HttpResponseMessageMock(this.httpStatus, this.headers, this.body);
        }
    }
}