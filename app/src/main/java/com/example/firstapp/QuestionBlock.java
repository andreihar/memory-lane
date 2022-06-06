package com.example.firstapp;

public class QuestionBlock {
    private Question[] questionArray = new Question[5];
    int image;

    // Constructor
    public QuestionBlock(Question first, Question second, Question third, Question fourth, Question fifth, int image) {
        questionArray[0] = first;
        questionArray[1] = second;
        questionArray[2] = third;
        questionArray[3] = fourth;
        questionArray[4] = fifth;
        this.image = image;
    }

    // Getters and Setters
    public Question getQuestionArray(int position) {
        return questionArray[position];
    }

    public void setQuestionArray(Question question, int position) {
        questionArray[position] = question;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

}
