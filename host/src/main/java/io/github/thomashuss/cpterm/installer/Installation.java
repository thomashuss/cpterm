package io.github.thomashuss.cpterm.installer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Installation {
    private List<File> files;
    private List<String> regs;

    public List<File> getFiles()
    {
        return files;
    }

    public List<String> getRegs()
    {
        return regs;
    }

    public void addFile(File file)
    {
        if (files == null) {
            files = new ArrayList<>(4);
        }
        files.add(file);
    }

    public void addReg(String key)
    {
        if (regs == null) {
            regs = new ArrayList<>(2);
        }
        regs.add(key);
    }
}
