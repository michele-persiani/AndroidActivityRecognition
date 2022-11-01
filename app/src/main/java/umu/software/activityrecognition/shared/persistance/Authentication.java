package umu.software.activityrecognition.shared.persistance;

import androidx.annotation.NonNull;

import java.io.Serializable;


public class Authentication implements Cloneable, Serializable
{
    private String username;
    private String password;


    public String getPassword()
    {
        return password;
    }

    public Authentication setPassword(String password)
    {
        this.password = password;
        return this;
    }

    public String getUsername()
    {
        return username;
    }


    public Authentication setUsername(String username)
    {
        this.username = username;
        return this;
    }


    @NonNull
    @Override
    public Authentication clone()
    {
        return new Authentication()
                .setPassword(password)
                .setUsername(username);
    }


    @Override
    public String toString()
    {
        return "Authentication{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    public static Authentication noAuthenticationRequired()
    {
        return new Authentication();
    }
}
