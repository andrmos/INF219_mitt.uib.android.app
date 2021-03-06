package no.mofifo.imber.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by PatrickFinseth on 16.02.16.
 */
public class Course {

    private int id;
    private String name;
    private String calender;
    private String course_code;
    @SerializedName("is_favorite")
    private boolean isFavorite;

    public Course(int id, String name, String calender, String courseCode){
        this.id = id;
        this.name = name;
        this.calender = calender;
        this.course_code = courseCode;
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getTrimmedName() {
        String trimmedName = name;

        String [] splitArray = name.split("/");

        if(splitArray.length > 1){
            trimmedName = splitArray[1].trim();
        }

        return trimmedName;
    }

    public String getCalenderUrl(){
        return calender;
    }

    public String getCourseCode(){
        return course_code;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public boolean equals(Course that) {
        if(this.getId() != that.getId()) {
            return false;
        }

        if (!this.getName().equals(that.getName())) {
            return false;
        }

        return this.getCourseCode().equals(that.getCourseCode());

    }

}
