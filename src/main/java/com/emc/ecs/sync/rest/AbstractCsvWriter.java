/*
 * Copyright 2013-2017 EMC Corporation. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.emc.ecs.sync.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public abstract class AbstractCsvWriter<T> implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ErrorReportWriter.class);

    public static final int BUFFER_SIZE = 128 * 1024;
    public static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss z";

    private Iterable<T> records;
    private PipedInputStream readStream;
    private Writer writer;
    protected DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

    public AbstractCsvWriter(Iterable<T> records) throws IOException {
        this.records = records;
        this.readStream = new PipedInputStream(BUFFER_SIZE);
        this.writer = new OutputStreamWriter(new PipedOutputStream(readStream));
    }

    protected abstract String[] getHeaders();

    protected abstract Object[] getColumns(T record);

    @Override
    public void run() {
        try {
            // header
            writeCsvRow((Object[]) getHeaders());

            // rows
            for (T record : records) {
                writeCsvRow((Object[]) getColumns(record));
            }

            writer.flush();
            writer.close();
        } catch (IOException e) {
            // don't close the stream, so the read end will get an exception and know there was a problem
            log.warn("Exception in stream writer!", e);
        }
    }

    private void writeCsvRow(Object... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) writer.write(",");
            if (values[i] != null) writer.write("\"" + escape(values[i].toString()) + "\"");
        }
        writer.write("\n");
    }

    private String escape(String value) {
        return value.replace("\"", "\"\"");
    }

    public InputStream getReadStream() {
        return readStream;
    }
}
