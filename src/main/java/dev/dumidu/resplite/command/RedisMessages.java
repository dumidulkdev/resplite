package dev.dumidu.resplite.command;

public final class RedisMessages {
    public static final String NOT_INTEGER = "ERR value is not an integer or out of range";
    public static final String WRONG_ARGS = "ERR wrong number of arguments for command";
    public static final String SYNTAX = "ERR syntax error";

    private RedisMessages() {
    }
}