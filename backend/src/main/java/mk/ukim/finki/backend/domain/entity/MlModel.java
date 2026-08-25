package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "ml_models")
public class MlModel {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String modelKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String checkpoint;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String inputAdapter;

    @Column(nullable = false)
    private boolean active = true;

    protected MlModel() {
        // JPA
    }

    public MlModel(String modelKey, String name, String checkpoint, String version, String inputAdapter) {
        this.modelKey = modelKey;
        this.name = name;
        this.checkpoint = checkpoint;
        this.version = version;
        this.inputAdapter = inputAdapter;
    }

    public UUID getId() {
        return id;
    }

    public String getModelKey() {
        return modelKey;
    }

    public String getName() {
        return name;
    }

    public String getCheckpoint() {
        return checkpoint;
    }

    public String getVersion() {
        return version;
    }

    public String getInputAdapter() {
        return inputAdapter;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
