package edu.example.library;

public abstract class Member {
    private final String id;

    protected Member(String id) {
        this.id = Identifiers.normalize(id);
    }

    public final String getId() {
        return id;
    }

    public abstract int borrowingLimit();

    public abstract int loanPeriodDays();
}
