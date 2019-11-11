package com.company;

public class States {
    public boolean isBsFinished() {
        return bsFinished;
    }

    public void setBsFinished(boolean bsFinished) {
        this.bsFinished = bsFinished;
    }

    public boolean isAnalyseFinished() {
        return analyseFinished;
    }

    public void setAnalyseFinished(boolean analyseFinished) {
        this.analyseFinished = analyseFinished;
    }

    private boolean bsFinished = true;
    private boolean analyseFinished = true;
}
