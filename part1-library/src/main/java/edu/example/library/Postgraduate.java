package edu.example.library;

public final class Postgraduate extends Member {
    public Postgraduate(String id) {
        super(id);
    }

    @Override
    public int borrowingLimit() {
        return 10;
    }

    @Override
    public int loanPeriodDays() {
        return 28;
    }
}
