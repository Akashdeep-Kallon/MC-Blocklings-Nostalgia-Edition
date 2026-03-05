package com.akah.blocklings.entity.blockling.skill;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.akah.blocklings.entity.blockling.skill.info.SkillInfo;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;

public class SkillsConfig
{
    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().toAbsolutePath();

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger(SkillsConfig.class);

    public static void read(String filename)
    {
        File file = new File(CONFIG_DIR.toString(), filename);

        try (JsonReader reader = new JsonReader(new FileReader(file)))
        {
            // [refactor-1.20.1] motivo: se mantiene la lectura para validación de formato sin retener objetos no usados.
            GSON.fromJson(reader, SkillInfo[].class);
        }
        catch (FileNotFoundException e)
        {
            // [refactor-1.20.1] motivo: sustituye printStackTrace por logging estructurado de Forge/Log4j.
            LOGGER.warn(e.getMessage(), e);
        }
        catch (Exception e)
        {
            // [refactor-1.20.1] motivo: evita fallos silenciosos al parsear configuraciones de skills.
            LOGGER.error("Failed to read skills config file: {}", file.getAbsolutePath(), e);
        }
    }
}
