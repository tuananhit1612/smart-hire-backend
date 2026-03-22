package com.smarthire.backend.features.report.service;

public interface ReportExportService {

    byte[] exportApplicationsCsv();

    byte[] exportJobsCsv();

    byte[] exportHrApplicationsCsv(Long userId);

    byte[] exportHrJobsCsv(Long userId);
}
