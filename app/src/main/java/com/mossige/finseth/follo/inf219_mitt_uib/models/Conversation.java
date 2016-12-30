package com.mossige.finseth.follo.inf219_mitt_uib.models;

import android.support.annotation.Nullable;

import java.util.ArrayList;

/**
 * Conversation model
 *
 * @author Øystein Follo
 * @date 16.02.2016
 */
public class Conversation {

    private String id;
    private String subject;
    private ArrayList<Participant> participants;
    @Nullable private ArrayList<Message> messages;
    private String last_message;

    /**
     * Used when you want all the messages in a conversation
     *
     * @param id String containing conversation id
     * @param subject String containing conversation subject
     * @param participants ArrayList containing the conversation {@link Participant participants}
     * @param messages ArrayList containing the conversation {@link Message messages}
     */
    public Conversation(String id, String subject, ArrayList<Participant> participants, ArrayList<Message> messages) {
        this.id = id;
        this.subject = subject;
        this.participants = participants;
        this.last_message = messages.get(messages.size()-1).getMessage();
        this.messages = messages;
    }

    /**
     * Used when you want last message of the conversation
     * Creates an empty ArrayList of messages
     *
     * @param id String containing conversation id
     * @param subject String containing conversation subject
     * @param participants ArrayList containing the conversation {@link Participant participants}
     * @param lastMessage String containg the last message in the conversation
     */
    public Conversation(String id, String subject, ArrayList<Participant> participants, String lastMessage) {
        this.id = id;
        this.subject = subject;
        this.participants = participants;
        this.last_message = lastMessage;
        this.messages = new ArrayList<>();
    }

    public String getLastMessage() {
        return last_message;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public String getSubject() {
        return subject;
    }

    public ArrayList<Participant> getParticipants() {
        return participants;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean equals(Conversation that) {
        if (!this.getId().equals(that.getId())) {
            return false;
        }

        if (!this.getSubject().equals(that.getSubject())) {
            return false;
        }

        if (!this.getLastMessage().equals(that.getLastMessage())) {
            return false;
        }

        return true;
    }
}
