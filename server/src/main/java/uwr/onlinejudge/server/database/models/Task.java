package uwr.onlinejudge.server.database.models;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Task {
    @Id
    @SequenceGenerator(name = "taskSequence", sequenceName = "task_sequence")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "taskSequence")
    private long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private TaskList taskList;

    @ManyToOne
    @JoinColumn(nullable = false)
    private SharedTask sharedTask;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deadline;

    @Lob
    private String comment;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public TaskList getTaskList() {
        return taskList;
    }

    public void setTaskList(TaskList taskList) {
        this.taskList = taskList;
    }

    public SharedTask getSharedTask() {
        return sharedTask;
    }

    public void setSharedTask(SharedTask sharedTask) {
        this.sharedTask = sharedTask;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}