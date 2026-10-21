package edu.example.library;

public final class Undergraduate extends Member {
    public Undergraduate(String id) {
        super(id);
    }

    @Override
    public int borrowingLimit() {
        return 5;
    }

    @Override
    public int loanPeriodDays() {
        return 14;
    }
}
