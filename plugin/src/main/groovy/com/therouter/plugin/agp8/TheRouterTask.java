package com.therouter.plugin.agp8;

import com.therouter.plugin.AddCodeVisitor;
import com.therouter.plugin.LogUI;
import com.therouter.plugin.TheRouterInjects;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public abstract class TheRouterTask extends TheRouterGetAllTask {

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    private JarOutputStream jarOutput;

    @Override
    public void taskAction() throws ClassNotFoundException, IOException {
        jarOutput = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(getOutputFile().get().getAsFile())));
        super.taskAction();
    }

    @Override
    public void onCacheChange() {
        //全量编译时完全不会读缓存，所以不处理
    }

    @Override
    public void mergeClassTransform(InputStream inputStream, String name) throws ClassNotFoundException, IOException {
        try (inputStream) {
            jarOutput.putNextEntry(new JarEntry(name));

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                jarOutput.write(buffer, 0, bytesRead);
            }

            jarOutput.closeEntry();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void asmTheRouterJar(File theRouterJar, JarEntry theRouterServiceProvideInjecter) throws IOException {
        if (theRouterJar != null && theRouterServiceProvideInjecter != null) {
            JarFile jarFile = new JarFile(theRouterJar);
            jarOutput.putNextEntry(new JarEntry(theRouterServiceProvideInjecter.getName()));
            ClassReader cr = new ClassReader(jarFile.getInputStream(theRouterServiceProvideInjecter));
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            AddCodeVisitor cv = new AddCodeVisitor(cw,
                    TheRouterInjects.serviceProvideMap,
                    TheRouterInjects.autowiredSet,
                    TheRouterInjects.routeSet,
                    false);
            cr.accept(cv, ClassReader.SKIP_DEBUG);
            byte[] bytes = cw.toByteArray();
            jarOutput.write(bytes);
            jarOutput.closeEntry();
            jarFile.close();
        }

        jarOutput.close();
    }
}