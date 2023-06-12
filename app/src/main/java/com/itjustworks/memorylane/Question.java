package com.itjustworks.memorylane;

/*
 * Question.java
 * 
 * Class Description: Takes the options for the text and stores in an array.
 *                    Identifies correct answer by index pointer.
 *                    Stores audio and text hints.
 * Class Invariant: Audio hint must be in the resources folder.
 *
 */

public class Question {
    private String question, hintText;
    private final String[] options = new String[4];
    private String hint;
    private int answer;

    // Constructor
    public Question(String question, String first, String second, String third, String fourth, String hintText, int answer, String hint) {
        this.question = question;
        options[0] = first;
        options[1] = second;
        options[2] = third;
        options[3] = fourth;
        this.hintText = hintText;
        this.answer = answer;
        this.hint = hint;
    }

    public Question() {
        this.question = "This is a default Question";
        options[0] = "first";
        options[1] = "second";
        options[2] = "third";
        options[3] = "fourth";
        this.hintText = "";
        this.answer = 0;
        this.hint = "";
    }

    // Getters and Setters
    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getOptions(int position) {
        return options[position];
    }

    public void setOptions(String option, int position) {
        options[position] = option;
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    public int getAnswer() {
        return answer;
    }

    public void setAnswer(int answer) {
        this.answer = answer;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getFirst() {
        return options[0];
    }

    public String getSecond() {
        return options[1];
    }

    public String getThird() {
        return options[2];
    }

    public String getFourth() {
        return options[3];
    }

    public void setFirst(String first) {
        this.options[0] = first;
    }

    public void setSecond(String second) {
        this.options[1] = second;
    }

    public void setThird(String third) {
        this.options[2] = third;
    }

    public void setFourth(String fourth) {
        this.options[3] = fourth;
    }
}
