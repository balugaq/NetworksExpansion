package com.ytdd9527.networksexpansion.implementation.machines.cellnet.storage.journal;
public record JournalRow(String cellUuid, char op, Long tplId, Long newAmount, int crc32, long timestamp) {}
