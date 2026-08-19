package mk.ukim.finki.backend.service.wfdb;

record WfdbSignalSpec(
        String fileName,
        int format,
        int byteOffset,
        double gain,
        int baseline,
        String unit,
        String leadName
) {
}
