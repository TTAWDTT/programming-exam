package edu.example.library;

import java.time.LocalDate;

record Loan(String memberId, String isbn, LocalDate borrowedOn,
            LocalDate dueOn, int periodDays) {}
