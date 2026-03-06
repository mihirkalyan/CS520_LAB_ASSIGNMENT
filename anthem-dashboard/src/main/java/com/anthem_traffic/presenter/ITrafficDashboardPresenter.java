package com.anthem_traffic.presenter;

public interface ITrafficDashboardPresenter {
    void onStartStreamClicked();
    void onStopStreamClicked();
    void onFetchHistoricalDataClicked();
    void onGenerateReportClicked();
}