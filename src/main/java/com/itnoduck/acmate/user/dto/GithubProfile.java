package com.itnoduck.acmate.user.dto;

/** Identity fetched from GitHub's authenticated /user endpoint, never supplied by the browser. */
public record GithubProfile(Long id, String login, String name, String verifiedEmail) implements java.io.Serializable {
    public GithubProfile(Long id, String login, String name) { this(id, login, name, null); }
}
