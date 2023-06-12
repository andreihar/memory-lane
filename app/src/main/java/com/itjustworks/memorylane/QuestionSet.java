package com.itjustworks.memorylane;

/*
 * QuestionSet.java
 * 
 * Class Description: Gets the questions as an input and stores in the array,
 *                    takes image for the questions.
 * Class Invariant: Image must be in the resources folder.
 *
 */

public class QuestionSet {
    private final Question[] questionArray = new Question[5];
    String image, video, id;
    private int counter, weight;
    private boolean complete;

    // Constructor
    public QuestionSet(Question q1, Question q2, Question q3, Question q4, Question q5, String image, String video, int counter, boolean complete, int weight) {
        questionArray[0] = q1;
        questionArray[1] = q2;
        questionArray[2] = q3;
        questionArray[3] = q4;
        questionArray[4] = q5;
        this.image = image;
        this.video = video;
        this.counter = counter;
        this.complete = complete;
        this.weight = weight;
    }

    public QuestionSet() {
        questionArray[0] = null;
        questionArray[1] = null;
        questionArray[2] = null;
        questionArray[3] = null;
        questionArray[4] = null;
        this.image = "";
        this.video = "";
        this.counter = 0;
        this.complete = false;
        this.weight = 50;
    }

    // Getters and Setters
    public Question getQuestionArray(int position) {
        return questionArray[position];
    }

    public void setQuestionArray(Question question, int position) {
        questionArray[position] = question;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public boolean getComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public Question getQ1() {
        return questionArray[0];
    }

    public Question getQ2() {
        return questionArray[1];
    }

    public Question getQ3() {
        return questionArray[2];
    }

    public Question getQ4() {
        return questionArray[3];
    }

    public Question getQ5() {
        return questionArray[4];
    }

    public void setQ1(Question q1) {
        this.questionArray[0] = q1;
    }

    public void setQ2(Question q2) {
        this.questionArray[1] = q2;
    }

    public void setQ3(Question q3) {
        this.questionArray[2] = q3;
    }

    public void setQ4(Question q4) {
        this.questionArray[3] = q4;
    }

    public void setQ5(Question q5) {
        this.questionArray[4] = q5;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = Math.max(weight, 1);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
