package edu.example.library;

interface SnapshotStore {
    void create(LibraryState state);

    LibraryState load();

    void save(LibraryState state);
}
