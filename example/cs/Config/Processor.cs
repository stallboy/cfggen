namespace Config;

public static class Processor
{
    private static readonly string Schema = ConfigPackage.SchemaKey(new ConfigReader(Convert.FromBase64String("CwAAAEEAAAAJAAAATGV2ZWxSYW5rCgAAAAACAAAABQAAAExldmVsAgAAAAQAAABSYW5rAgAAAAgAAABQb3NpdGlvbgoAAAAAAwAAAAEAAAB4AgAAAAEAAAB5AgAAAAEAAAB6AgAAAAUAAABSYW5nZQoAAAAAAgAAAAMAAABNaW4CAAAAAwAAAE1heAIAAAAOAAAAYWkuVHJpZ2dlclRpY2sLAAAAAwAAAAoAAABDb25zdFZhbHVlCgAAAAABAAAABQAAAHZhbHVlAgAAAAcAAABCeUxldmVsCgAAAAACAAAABAAAAGluaXQCAAAACwAAAGNvZWZmaWNpZW50BAAAAA0AAABCeVNlcnZlclVwRGF5CgAAAAADAAAABAAAAGluaXQCAAAADAAAAGNvZWZmaWNpZW50MQQAAAAMAAAAY29lZmZpY2llbnQyBAAAABEAAABjb21iYXQuQWJpbGl0eVVzZQoAAAAAAgAAAAcAAABiaW5kaW5nBwAAAA4AAABjb21iYXQuQmluZGluZwgAAABtYWNoaW5lcwgAAAAHAAAAEQAAAGNvbWJhdC5NYWNoaW5lVXNlDQAAAGNvbWJhdC5BbmNob3IKAAAAAAAAAAAQAAAAY29tYmF0LkFuY2hvclVzZQoAAAAAAAAAAA4AAABjb21iYXQuQmluZGluZwoAAAAAAQAAAAkAAABhYmlsaXR5SWQCAAAAEAAAAGNvbWJhdC5Cb29rRW50cnkKAAAAAAEAAAAEAAAAbmFtZQUAAAAMAAAAY29tYmF0LkNvbWJvCgAAAAAEAAAABgAAAHdlYXBvbgUAAAAEAAAAdGllcgIAAAAIAAAAYWlyYm9ybmUBAAAABgAAAGVmZmVjdAcAAAANAAAAY29tYmF0LkVmZmVjdA4AAABjb21iYXQuQ29tbWFuZAsAAAADAAAADAAAAFN0YXRlTWFjaGluZQoAAAAAAQAAAAYAAABwaGFzZXMIAAAABwAAABIAAABjb21iYXQuTmVzdGVkUGhhc2UEAAAASnVtcAoAAAAAAQAAAAMAAABrZXkFAAAABAAAAE5vb3AKAAAAAAAAAAANAAAAY29tYmF0LkVmZmVjdAsAAAADAAAABgAAAERhbWFnZQoAAAAAAQAAAAYAAABhbW91bnQCAAAABAAAAEhlYWwKAAAAAAEAAAAGAAAAYW1vdW50AgAAAAQAAABOb25lCgAAAAAAAAAACwAAAGNvbWJhdC5FeGl0CgAAAAABAAAABgAAAGVmZmVjdAcAAAANAAAAY29tYmF0LkVmZmVjdAwAAABjb21iYXQuRnJhbWUKAAAAAAIAAAAEAAAAdGljawMAAAAGAAAAZWZmZWN0BwAAAA0AAABjb21iYXQuRWZmZWN0DQAAAGNvbWJhdC5JbXBhY3QKAAAAAAIAAAAHAAAAb3JkaW5hbAIAAAAGAAAAZGFtYWdlAgAAAA4AAABjb21iYXQuTWFjaGluZQoAAAAAEwAAAAQAAABjb2RlBQAAAAYAAABwaGFzZXMIAAAABwAAAAwAAABjb21iYXQuUGhhc2UHAAAAaW1wYWN0cwgAAAAHAAAADQAAAGNvbWJhdC5JbXBhY3QGAAAAZnJhbWVzCAAAAAcAAAAMAAAAY29tYmF0LkZyYW1lBwAAAHN0YW5jZXMIAAAABwAAAA0AAABjb21iYXQuU3RhbmNlCAAAAHZhcmlhbnRzCAAAAAcAAAATAAAAY29tYmF0LlBoYXNlVmFyaWFudAYAAABjb21ib3MIAAAABwAAAAwAAABjb21iYXQuQ29tYm8HAAAAbWFya2VycwgAAAAHAAAADQAAAGNvbWJhdC5NYXJrZXIGAAAAYW5jaG9yBwAAAA0AAABjb21iYXQuQW5jaG9yBwAAAGFuY2hvcnMIAAAABwAAABAAAABjb21iYXQuQW5jaG9yVXNlBgAAAG9uRXhpdAcAAAALAAAAY29tYmF0LkV4aXQHAAAAaW5pdGlhbAUAAAAGAAAAaW1wYWN0AgAAAAUAAABmcmFtZQMAAAAGAAAAc3RhbmNlAQAAAAcAAAB2YXJpYW50BwAAAA8AAABjb21iYXQuUGhhc2VLZXkGAAAAd2VhcG9uBQAAAAQAAAB0aWVyAgAAAAgAAABhaXJib3JuZQEAAAARAAAAY29tYmF0Lk1hY2hpbmVVc2UKAAAAAAIAAAAKAAAAbWFjaGluZUtleQUAAAAHAAAAdGFyZ2V0cwgAAAAHAAAADwAAAGNvbWJhdC5QaGFzZVVzZQ0AAABjb21iYXQuTWFya2VyCgAAAAABAAAAAwAAAGtleQUAAAASAAAAY29tYmF0Lk5lc3RlZFBoYXNlCgAAAAACAAAAAwAAAGtleQUAAAAGAAAAYWN0aW9uBwAAAA4AAABjb21iYXQuQ29tbWFuZBIAAABjb21iYXQuT3B0aW9uYWxBaW0KAAAAAAMAAAAJAAAAYWJpbGl0eUlkAgAAAAoAAABtYWNoaW5lS2V5BQAAAAgAAABwaGFzZUtleQUAAAANAAAAY29tYmF0Lk9yaWdpbgoAAAAAAgAAAAEAAAB4AgAAAAEAAAB5AgAAAAwAAABjb21iYXQuUGhhc2UKAAAAAAIAAAADAAAAa2V5BQAAAAQAAABuZXh0BQAAAA8AAABjb21iYXQuUGhhc2VLZXkKAAAAAAIAAAAEAAAAbmFtZQUAAAAHAAAAdmFyaWFudAIAAAAPAAAAY29tYmF0LlBoYXNlVXNlCgAAAAABAAAACAAAAHBoYXNlS2V5BQAAABMAAABjb21iYXQuUGhhc2VWYXJpYW50CgAAAAACAAAAAwAAAGtleQcAAAAPAAAAY29tYmF0LlBoYXNlS2V5BgAAAGVmZmVjdAcAAAANAAAAY29tYmF0LkVmZmVjdA0AAABjb21iYXQuUmV3YXJkCgAAAAABAAAABQAAAGNvdW50AgAAAA0AAABjb21iYXQuU3RhbmNlCgAAAAACAAAACAAAAGFpcmJvcm5lAQAAAAYAAABlZmZlY3QHAAAADQAAAGNvbWJhdC5FZmZlY3QSAAAAZXF1aXAuVGVzdFBhY2tCZWFuCgAAAAACAAAABAAAAG5hbWUFAAAABgAAAGlSYW5nZQcAAAAFAAAAUmFuZ2UOAAAAb3RoZXIuRHJvcEl0ZW0KAAAAAAQAAAAGAAAAY2hhbmNlAgAAAAcAAABpdGVtaWRzCAAAAAIAAAAIAAAAY291bnRtaW4CAAAACAAAAGNvdW50bWF4AgAAABQAAAB0YXNrLlRlc3REZWZhdWx0QmVhbgoAAAAABwAAAAcAAAB0ZXN0SW50AgAAAAgAAAB0ZXN0Qm9vbAEAAAAKAAAAdGVzdFN0cmluZwUAAAALAAAAdGVzdFN1YkJlYW4HAAAACAAAAFBvc2l0aW9uCAAAAHRlc3RMaXN0CAAAAAIAAAAJAAAAdGVzdExpc3QyCAAAAAIAAAAHAAAAdGVzdE1hcAkAAAACAAAABQAAABYAAAB0YXNrLmNvbXBsZXRlY29uZGl0aW9uCwAAAAcAAAALAAAAS2lsbE1vbnN0ZXIKAAAAAAIAAAAJAAAAbW9uc3RlcmlkAgAAAAUAAABjb3VudAIAAAAHAAAAVGFsa05wYwoAAAAAAQAAAAUAAABucGNpZAIAAAAMAAAAVGVzdE5vQ29sdW1uCgAAAAAAAAAABAAAAENoYXQKAAAAAAEAAAADAAAAbXNnBQAAAAwAAABDb25kaXRpb25BbmQKAAAAAAIAAAAFAAAAY29uZDEHAAAAFgAAAHRhc2suY29tcGxldGVjb25kaXRpb24FAAAAY29uZDIHAAAAFgAAAHRhc2suY29tcGxldGVjb25kaXRpb24LAAAAQ29sbGVjdEl0ZW0KAAAAAAIAAAAGAAAAaXRlbWlkAgAAAAUAAABjb3VudAIAAAACAAAAYWEKAAAAAAAAAAAFAAAAYWkuYWkKAAAAAQcAAAACAAAASUQCAAAABAAAAERlc2MFAAAABgAAAENvbmRJRAUAAAAIAAAAVHJpZ1RpY2sHAAAADgAAAGFpLlRyaWdnZXJUaWNrCAAAAFRyaWdPZGRzAgAAAAgAAABBY3Rpb25JRAgAAAACAAAACwAAAERlYXRoUmVtb3ZlAQAAAAwAAABhaS5haV9hY3Rpb24KAAAAAQUAAAACAAAASUQCAAAABAAAAERlc2MFAAAACQAAAEZvcm11bGFJRAIAAAAIAAAAQXJnSUxpc3QIAAAAAgAAAAgAAABBcmdTTGlzdAgAAAACAAAADwAAAGFpLmFpX2NvbmRpdGlvbgoAAAABBQAAAAIAAABJRAIAAAAEAAAARGVzYwUAAAAJAAAARm9ybXVsYUlEAgAAAAgAAABBcmdJTGlzdAgAAAACAAAACAAAAEFyZ1NMaXN0CAAAAAIAAAAOAAAAY29tYmF0LmFiaWxpdHkKAAAAAQYAAAACAAAAaWQCAAAABAAAAGNvZGUFAAAABgAAAG9yaWdpbgcAAAANAAAAY29tYmF0Lk9yaWdpbggAAABtYWNoaW5lcwgAAAAHAAAADgAAAGNvbWJhdC5NYWNoaW5lDAAAAHN0YXJ0TWFjaGluZQUAAAAKAAAAc3RhcnRQaGFzZQUAAAATAAAAY29tYmF0LmJyYW5jaHJld2FyZAoAAAABAgAAAAMAAABrZXkHAAAADwAAAGNvbWJhdC5QaGFzZUtleQYAAAByZXdhcmQHAAAADQAAAGNvbWJhdC5SZXdhcmQLAAAAY29tYmF0LmNhc3QKAAAAAQsAAAACAAAAaWQCAAAACwAAAGFiaWxpdHlDb2RlBQAAAAYAAABvcmlnaW4HAAAADQAAAGNvbWJhdC5PcmlnaW4EAAAAdXNlcwgAAAAHAAAAEQAAAGNvbWJhdC5BYmlsaXR5VXNlCAAAAG9wdGlvbmFsCAAAAAcAAAASAAAAY29tYmF0Lk9wdGlvbmFsQWltBgAAAHNjaG9vbAUAAAAEAAAAdGllcgIAAAAIAAAAYWlyYm9ybmUBAAAACAAAAGJvb2tDb2RlBQAAAAcAAABlZGl0aW9uAgAAAAkAAAByZXdhcmRLZXkHAAAADwAAAGNvbWJhdC5QaGFzZUtleRAAAABjb21iYXQuZW5jb3VudGVyCgAAAAECAAAAAgAAAGlkAgAAAAcAAABjb21tYW5kBwAAAA4AAABjb21iYXQuQ29tbWFuZBAAAABjb21iYXQucGhhc2Vib29rCgAAAAEGAAAABgAAAHNjaG9vbAUAAAAEAAAAdGllcgIAAAAIAAAAYWlyYm9ybmUBAAAABAAAAGNvZGUFAAAABwAAAGVkaXRpb24CAAAABQAAAGVudHJ5BwAAABAAAABjb21iYXQuQm9va0VudHJ5DwAAAGNvbWJhdC50aW1lbGluZQoAAAABDAAAAAIAAABpZAIAAAAGAAAAZXZlbnRzCQAAAAcAAAAPAAAAY29tYmF0LlBoYXNlS2V5BwAAAA0AAABjb21iYXQuRWZmZWN0CAAAAGNoYW5uZWxzCQAAAAUAAAAHAAAADQAAAGNvbWJhdC5FZmZlY3QFAAAAc2xvdHMJAAAAAgAAAAcAAAANAAAAY29tYmF0LkVmZmVjdAUAAABjbG9jawkAAAADAAAABwAAAA0AAABjb21iYXQuRWZmZWN0BQAAAG1vZGVzCQAAAAEAAAAHAAAADQAAAGNvbWJhdC5FZmZlY3QIAAAAZXZlbnRLZXkHAAAADwAAAGNvbWJhdC5QaGFzZUtleQcAAABjaGFubmVsBQAAAAQAAABzbG90AgAAAAUAAABmcmFtZQMAAAAIAAAAYWlyYm9ybmUBAAAACAAAAGZhbGxiYWNrBwAAAA0AAABjb21iYXQuRWZmZWN0DQAAAGVxdWlwLmFiaWxpdHkMAAAAAAEHAAAABgAAAGF0dGFjawEAAAAHAAAAZGVmZW5jZQIAAAACAAAAaHADAAAACAAAAGNyaXRpY2FsBAAAAA8AAABjcml0aWNhbF9yZXNpc3QFAAAABQAAAGJsb2NrBgAAAAsAAABicmVha19hcm1vcgcAAAAUAAAAZXF1aXAuYWJpbGl0eV9EZXRhaWwKAAAAAQIAAAACAAAAaWQCAAAABAAAAG5hbWUFAAAAEgAAAGVxdWlwLmVxdWlwX2NvbmZpZwoAAAABBgAAAAUAAABlbnRyeQUAAAATAAAAc3RvbmVfY291bnRfZm9yX3NldAIAAAARAAAAZHJhd19wcm90ZWN0X25hbWUFAAAACwAAAGJyb2FkY2FzdGlkAgAAABcAAABicm9hZGNhc3RfbGVhc3RfcXVhbGl0eQIAAAASAAAAd2Vla19yZXdhcmRfbWFpbGlkAgAAABgAAABlcXVpcC5lcXVpcF9jb25maWdfRW50cnkMAAAAAQACAAAACAAAAEluc3RhbmNlCQAAAEluc3RhbmNlMg0AAABlcXVpcC5qZXdlbHJ5CgAAAAEKAAAAAgAAAElEAgAAAAQAAABOYW1lBQAAAAgAAABJY29uRmlsZQUAAAAHAAAATHZsUmFuawcAAAAJAAAATGV2ZWxSYW5rBQAAAEpUeXBlBQAAAAYAAABTdWl0SUQCAAAACgAAAEtleUFiaWxpdHkCAAAADwAAAEtleUFiaWxpdHlWYWx1ZQIAAAAJAAAAU2FsZVByaWNlAgAAAAsAAABEZXNjcmlwdGlvbgUAAAAUAAAAZXF1aXAuamV3ZWxyeV9yYW5kb20KAAAAAQQAAAAHAAAATHZsUmFuawcAAAAJAAAATGV2ZWxSYW5rCwAAAEF0dGFja1JhbmdlBwAAAAUAAABSYW5nZQoAAABPdGhlclJhbmdlCAAAAAcAAAAFAAAAUmFuZ2UIAAAAVGVzdFBhY2sIAAAABwAAABIAAABlcXVpcC5UZXN0UGFja0JlYW4SAAAAZXF1aXAuamV3ZWxyeV9zdWl0CgAAAAEKAAAABgAAAFN1aXRJRAIAAAAFAAAARW5hbWUFAAAABAAAAE5hbWUGAAAACAAAAEFiaWxpdHkxAgAAAA0AAABBYmlsaXR5MVZhbHVlAgAAAAgAAABBYmlsaXR5MgIAAAANAAAAQWJpbGl0eTJWYWx1ZQIAAAAIAAAAQWJpbGl0eTMCAAAADQAAAEFiaWxpdHkzVmFsdWUCAAAACAAAAFN1aXRMaXN0CAAAAAIAAAAYAAAAZXF1aXAuamV3ZWxyeV9zdWl0X0VudHJ5DAAAAAEBAQAAAAsAAABTcGVjaWFsU3VpdAQAAAASAAAAZXF1aXAuamV3ZWxyeV90eXBlDAAAAAAABAAAAAQAAABKYWRlCAAAAEJyYWNlbGV0BQAAAE1hZ2ljBgAAAEJvdHRsZRkAAABlcXVpcC5qZXdlbHJ5X3R5cGVfRGV0YWlsCgAAAAEBAAAACAAAAFR5cGVOYW1lBQAAAAoAAABlcXVpcC5yYW5rDAAAAAABBgAAAAUAAAB3aGl0ZQAAAAAFAAAAZ3JlZW4BAAAABAAAAGJsdWUCAAAABgAAAHB1cnBsZQMAAAAGAAAAeWVsbG93BAAAAAMAAAByZWQFAAAAEQAAAGVxdWlwLnJhbmtfRGV0YWlsCgAAAAEDAAAABgAAAFJhbmtJRAIAAAAIAAAAUmFua05hbWUFAAAADAAAAFJhbmtTaG93TmFtZQUAAAAUAAAAb3RoZXIuQXJnQ2FwdHVyZU1vZGUMAAAAAAACAAAACAAAAFNuYXBzaG90BwAAAER5bmFtaWMbAAAAb3RoZXIuQXJnQ2FwdHVyZU1vZGVfRGV0YWlsCgAAAAEDAAAABAAAAG5hbWUFAAAAAgAAAGlkAgAAAAcAAABjb21tZW50BQAAAAoAAABvdGhlci5kcm9wCgAAAAEEAAAABgAAAGRyb3BpZAIAAAAEAAAAbmFtZQYAAAAFAAAAaXRlbXMIAAAABwAAAA4AAABvdGhlci5Ecm9wSXRlbQcAAAB0ZXN0bWFwCQAAAAIAAAACAAAADgAAAG90aGVyLmtleV90ZXN0CgAAAAEGAAAAAwAAAGlkMQIAAAADAAAAaWQyAwAAAAMAAABpZDMCAAAAAwAAAGlkcwgAAAACAAAACAAAAGVudW1UZXN0BQAAAAgAAABlbnVtTGlzdAgAAAAFAAAACgAAAG90aGVyLmxvb3QKAAAAAQQAAAAGAAAAbG9vdGlkAgAAAAUAAABlbmFtZQUAAAAEAAAAbmFtZQYAAAAKAAAAY2hhbmNlTGlzdAgAAAACAAAADwAAAG90aGVyLmxvb3RfaXRlbQoAAAABBQAAAAYAAABsb290aWQCAAAABgAAAGl0ZW1pZAIAAAAGAAAAY2hhbmNlAgAAAAgAAABjb3VudG1pbgIAAAAIAAAAY291bnRtYXgCAAAADQAAAG90aGVyLm1vbnN0ZXIKAAAAAQYAAAACAAAAaWQCAAAABwAAAHBvc0xpc3QIAAAABwAAAAgAAABQb3NpdGlvbgYAAABsb290SWQCAAAACgAAAGxvb3RJdGVtSWQCAAAACAAAAGVudW1NYXAxCQAAAAUAAAACAAAACAAAAGVudW1NYXAyCQAAAAIAAAAFAAAADQAAAG90aGVyLnNpZ25faW4KAAAAAQUAAAACAAAAaWQCAAAADQAAAGl0ZW0yY291bnRNYXAJAAAAAgAAAAIAAAATAAAAdmlwaXRlbTJ2aXBjb3VudE1hcAkAAAACAAAAAgAAAAgAAAB2aXBsZXZlbAIAAAAIAAAASWNvbkZpbGUFAAAAHAAAAHRhc2suY29tcGxldGVfY29uZGl0aW9uX3R5cGUMAAAAAAEHAAAACwAAAEtpbGxNb25zdGVyAQAAAAcAAABUYWxrTnBjAgAAAAsAAABDb2xsZWN0SXRlbQMAAAAMAAAAQ29uZGl0aW9uQW5kBAAAAAQAAABDaGF0BQAAAAwAAABUZXN0Tm9Db2x1bW4GAAAAAgAAAGFhBwAAACMAAAB0YXNrLmNvbXBsZXRlX2NvbmRpdGlvbl90eXBlX0RldGFpbAoAAAABAgAAAAIAAABpZAIAAAAEAAAAbmFtZQUAAAAJAAAAdGFzay50YXNrCgAAAAEGAAAABgAAAHRhc2tpZAIAAAAEAAAAbmFtZQgAAAAGAAAACAAAAG5leHR0YXNrAgAAABEAAABjb21wbGV0ZWNvbmRpdGlvbgcAAAAWAAAAdGFzay5jb21wbGV0ZWNvbmRpdGlvbgMAAABleHACAAAADwAAAHRlc3REZWZhdWx0QmVhbgcAAAAUAAAAdGFzay5UZXN0RGVmYXVsdEJlYW4KAAAAdGFzay50YXNrMgoAAAABCwAAAAYAAAB0YXNraWQCAAAABAAAAG5hbWUIAAAABgAAAAgAAABuZXh0dGFzawIAAAARAAAAY29tcGxldGVjb25kaXRpb24HAAAAFgAAAHRhc2suY29tcGxldGVjb25kaXRpb24DAAAAZXhwAgAAAAgAAAB0ZXN0Qm9vbAEAAAAKAAAAdGVzdFN0cmluZwUAAAAKAAAAdGVzdFN0cnVjdAcAAAAIAAAAUG9zaXRpb24IAAAAdGVzdExpc3QIAAAAAgAAAA4AAAB0ZXN0TGlzdFN0cnVjdAgAAAAHAAAACAAAAFBvc2l0aW9uEQAAAHRlc3RMaXN0SW50ZXJmYWNlCAAAAAcAAAAOAAAAYWkuVHJpZ2dlclRpY2sTAAAAdGFzay50YXNrX2V4dHJhX2V4cAoAAAABCAAAAAYAAAB0YXNraWQCAAAACAAAAGV4dHJhZXhwAgAAAAUAAAB0ZXN0MQUAAAAFAAAAdGVzdDIFAAAABgAAAGZpZWxkYQUAAAAGAAAAZmllbGRiBQAAAAYAAABmaWVsZGMFAAAABgAAAGZpZWxkZAUAAAA=")));
    private static readonly HashSet<string> TableBlocks = new HashSet<string>(StringComparer.Ordinal) {
        ".ai.ai",
        ".ai.ai_action",
        ".ai.ai_condition",
        ".combat.ability",
        ".combat.branchreward",
        ".combat.cast",
        ".combat.encounter",
        ".combat.phasebook",
        ".combat.timeline",
        ".equip.ability",
        ".equip.equip_config",
        ".equip.jewelry",
        ".equip.jewelry_random",
        ".equip.jewelry_suit",
        ".equip.jewelry_type",
        ".equip.rank",
        ".other.ArgCaptureMode",
        ".other.drop",
        ".other.key_test",
        ".other.loot",
        ".other.loot_item",
        ".other.monster",
        ".other.sign_in",
        ".task.complete_condition_type",
        ".task.task",
        ".task.task2",
        ".task.task_extra_exp",
    };
    public static ConfigMgr LoadPackagesAndSetMgr(params ConfigPackage[] packages)
    {
        var config = LoadPackages(packages);
        ConfigMgr.SetMgr(config);
        return config;
    }

    public static ConfigMgr LoadPackages(params ConfigPackage[] packages)
    {
        ConfigPackage.ValidateOrder(packages);
        var errors = new LoadErrors();
        foreach (var pack in packages) {
            pack.ValidateSchema(Schema, errors);
            foreach (string block in pack.BlockNames) {
                if (block.StartsWith(".", StringComparison.Ordinal) && !TableBlocks.Contains(block)) {
                    errors.UnknownTableBlock(pack.Name, block);
                }
            }
        }
        errors.CheckErrors();
        var config = new ConfigMgr();
        {
            var rows = ReadTable(packages, "ai.ai", global::Config.Ai.DAi._create,
                global::Config.Ai.DAi.PrimaryKey, config._ai_dai_Origins, errors);
            global::Config.Ai.DAi.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "ai.ai_action", global::Config.Ai.DAiAction._create,
                global::Config.Ai.DAiAction.PrimaryKey, config._ai_dai_uaction_Origins, errors);
            global::Config.Ai.DAiAction.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "ai.ai_condition", global::Config.Ai.DAiCondition._create,
                global::Config.Ai.DAiCondition.PrimaryKey, config._ai_dai_ucondition_Origins, errors);
            global::Config.Ai.DAiCondition.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "combat.ability", global::Config.Combat.DAbility._create,
                global::Config.Combat.DAbility.PrimaryKey, config._combat_dability_Origins, errors);
            global::Config.Combat.DAbility.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "combat.branchreward", global::Config.Combat.DBranchreward._create,
                global::Config.Combat.DBranchreward.PrimaryKey, config._combat_dbranchreward_Origins, errors);
            global::Config.Combat.DBranchreward.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "combat.cast", global::Config.Combat.DCast._create,
                global::Config.Combat.DCast.PrimaryKey, config._combat_dcast_Origins, errors);
            global::Config.Combat.DCast.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "combat.encounter", global::Config.Combat.DEncounter._create,
                global::Config.Combat.DEncounter.PrimaryKey, config._combat_dencounter_Origins, errors);
            global::Config.Combat.DEncounter.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "combat.phasebook", global::Config.Combat.DPhasebook._create,
                global::Config.Combat.DPhasebook.PrimaryKey, config._combat_dphasebook_Origins, errors);
            global::Config.Combat.DPhasebook.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "combat.timeline", global::Config.Combat.DTimeline._create,
                global::Config.Combat.DTimeline.PrimaryKey, config._combat_dtimeline_Origins, errors);
            global::Config.Combat.DTimeline.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.ability", global::Config.Equip.DAbilityInfo._create,
                global::Config.Equip.DAbilityInfo.PrimaryKey, config._equip_dability_Origins, errors);
            global::Config.Equip.DAbilityInfo.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.equip_config", global::Config.Equip.DEquipConfig._create,
                global::Config.Equip.DEquipConfig.PrimaryKey, config._equip_dequip_uconfig_Origins, errors);
            global::Config.Equip.DEquipConfig.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelry", global::Config.Equip.DJewelry._create,
                global::Config.Equip.DJewelry.PrimaryKey, config._equip_djewelry_Origins, errors);
            global::Config.Equip.DJewelry.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelry_random", global::Config.Equip.DJewelryRandom._create,
                global::Config.Equip.DJewelryRandom.PrimaryKey, config._equip_djewelry_urandom_Origins, errors);
            global::Config.Equip.DJewelryRandom.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelry_suit", global::Config.Equip.DJewelrySuit._create,
                global::Config.Equip.DJewelrySuit.PrimaryKey, config._equip_djewelry_usuit_Origins, errors);
            global::Config.Equip.DJewelrySuit.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelry_type", global::Config.Equip.DJewelryTypeInfo._create,
                global::Config.Equip.DJewelryTypeInfo.PrimaryKey, config._equip_djewelry_utype_Origins, errors);
            global::Config.Equip.DJewelryTypeInfo.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.rank", global::Config.Equip.DRankInfo._create,
                global::Config.Equip.DRankInfo.PrimaryKey, config._equip_drank_Origins, errors);
            global::Config.Equip.DRankInfo.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.ArgCaptureMode", global::Config.Other.DArgCaptureModeInfo._create,
                global::Config.Other.DArgCaptureModeInfo.PrimaryKey, config._other_dArgCaptureMode_Origins, errors);
            global::Config.Other.DArgCaptureModeInfo.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.drop", global::Config.Other.DDrop._create,
                global::Config.Other.DDrop.PrimaryKey, config._other_ddrop_Origins, errors);
            global::Config.Other.DDrop.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.key_test", global::Config.Other.DKeyTest._create,
                global::Config.Other.DKeyTest.PrimaryKey, config._other_dkey_utest_Origins, errors);
            global::Config.Other.DKeyTest.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.loot", global::Config.Other.DLoot._create,
                global::Config.Other.DLoot.PrimaryKey, config._other_dloot_Origins, errors);
            global::Config.Other.DLoot.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.loot_item", global::Config.Other.DLootItem._create,
                global::Config.Other.DLootItem.PrimaryKey, config._other_dloot_uitem_Origins, errors);
            global::Config.Other.DLootItem.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.monster", global::Config.Other.DMonster._create,
                global::Config.Other.DMonster.PrimaryKey, config._other_dmonster_Origins, errors);
            global::Config.Other.DMonster.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.sign_in", global::Config.Other.DSignIn._create,
                global::Config.Other.DSignIn.PrimaryKey, config._other_dsign_uin_Origins, errors);
            global::Config.Other.DSignIn.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "task.complete_condition_type", global::Config.Task.DCompleteConditionTypeInfo._create,
                global::Config.Task.DCompleteConditionTypeInfo.PrimaryKey, config._task_dcomplete_ucondition_utype_Origins, errors);
            global::Config.Task.DCompleteConditionTypeInfo.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "task.task", global::Config.Task.DTask._create,
                global::Config.Task.DTask.PrimaryKey, config._task_dtask_Origins, errors);
            global::Config.Task.DTask.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "task.task2", global::Config.Task.DTask2._create,
                global::Config.Task.DTask2.PrimaryKey, config._task_dtask2_Origins, errors);
            global::Config.Task.DTask2.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "task.task_extra_exp", global::Config.Task.DTaskExtraExp._create,
                global::Config.Task.DTaskExtraExp.PrimaryKey, config._task_dtask_uextra_uexp_Origins, errors);
            global::Config.Task.DTaskExtraExp.InitializeAll(rows, errors, config);
        }
        errors.CheckErrors();
        foreach (var row in global::Config.Combat.DAbility.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Combat.DCast.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Combat.DEncounter.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Combat.DTimeline.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Equip.DJewelry.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Equip.DJewelryRandom.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DKeyTest.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DLoot.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DMonster.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DSignIn.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Task.DTask.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Task.DTask2.All(config)) {
            row._resolve(errors, config);
        }
        errors.CheckErrors();
        ResolveRelations(config, errors);
        errors.CheckErrors();
        foreach (var row in global::Config.Ai.DAi.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Ai.DAiAction.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Ai.DAiCondition.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DAbility.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DBranchreward.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DCast.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DEncounter.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DPhasebook.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Combat.DTimeline.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DAbilityInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DEquipConfig.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelry.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelryRandom.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelrySuit.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelryTypeInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DRankInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DArgCaptureModeInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DDrop.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DKeyTest.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DLoot.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DLootItem.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DMonster.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DSignIn.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DCompleteConditionTypeInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DTask.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DTask2.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DTaskExtraExp.All(config)) {
            Ready(row, config);
        }
        config.MarkReady();
        return config;
    }

    private static void Ready<T>(T value, ConfigMgr config)
    {
        if (value is IConfigExtension extension) {
            extension.OnConfigReady(config);
        }
    }

    private static void ResolveRelations(ConfigMgr config, LoadErrors errors)
{
    var resolver = new RelationResolver(errors, new RelationResolver.Shape[] {
        new RelationResolver.Shape("LevelRank", typeof(global::Config.DLevelRank),
            value => { var row = (global::Config.DLevelRank)value; return new object[] { row.Level, row.Rank }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("Position", typeof(global::Config.DPosition),
            value => { var row = (global::Config.DPosition)value; return new object[] { row.X, row.Y, row.Z }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("Range", typeof(global::Config.DRange),
            value => { var row = (global::Config.DRange)value; return new object[] { row.Min, row.Max }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("ai.TriggerTick.ByLevel", typeof(global::Config.Ai.TriggerTick.DByLevel),
            value => { var row = (global::Config.Ai.TriggerTick.DByLevel)value; return new object[] { row.Init, row.Coefficient }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("ai.TriggerTick.ByServerUpDay", typeof(global::Config.Ai.TriggerTick.DByServerUpDay),
            value => { var row = (global::Config.Ai.TriggerTick.DByServerUpDay)value; return new object[] { row.Init, row.Coefficient1, row.Coefficient2 }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("ai.TriggerTick.ConstValue", typeof(global::Config.Ai.TriggerTick.DConstValue),
            value => { var row = (global::Config.Ai.TriggerTick.DConstValue)value; return new object[] { row.Value }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("ai.ai", typeof(global::Config.Ai.DAi),
            value => { var row = (global::Config.Ai.DAi)value; return new object[] { row.ID, row.Desc, row.CondID, row.TrigTick, row.TrigOdds, row.ActionID, row.DeathRemove }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("ai.ai_action", typeof(global::Config.Ai.DAiAction),
            value => { var row = (global::Config.Ai.DAiAction)value; return new object[] { row.ID, row.Desc, row.FormulaID, row.ArgIList, row.ArgSList }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("ai.ai_condition", typeof(global::Config.Ai.DAiCondition),
            value => { var row = (global::Config.Ai.DAiCondition)value; return new object[] { row.ID, row.Desc, row.FormulaID, row.ArgIList, row.ArgSList }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.AbilityUse", typeof(global::Config.Combat.DAbilityUse),
            value => { var row = (global::Config.Combat.DAbilityUse)value; return new object[] { row.Binding, row.Machines }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
                new RelationResolver.Plan("combat.cast", 0, "ability", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{0, 0}),
                    }, "table:combat.ability", new int[]{0}, new RelationResolver.Step[] {
                    }),
            }, (value, refs) => {
                var row = (global::Config.Combat.DAbilityUse)value;
                row.RefAbility = (global::Config.Combat.DAbility)refs[0]!;
            }),
        new RelationResolver.Shape("combat.Anchor", typeof(global::Config.Combat.DAnchor),
            value => { var row = (global::Config.Combat.DAnchor)value; return new object[] {  }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.AnchorUse", typeof(global::Config.Combat.DAnchorUse),
            value => { var row = (global::Config.Combat.DAnchorUse)value; return new object[] {  }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
                new RelationResolver.Plan("combat.ability", 0, "anchor", false,
                    new RelationResolver.Source[] {
                    }, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(8, 0, new int[]{}),
                    }),
            }, (value, refs) => {
                var row = (global::Config.Combat.DAnchorUse)value;
                row.RefAnchor = (global::Config.Combat.DAnchor)refs[0]!;
            }),
        new RelationResolver.Shape("combat.Binding", typeof(global::Config.Combat.DBinding),
            value => { var row = (global::Config.Combat.DBinding)value; return new object[] { row.AbilityId }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.BookEntry", typeof(global::Config.Combat.DBookEntry),
            value => { var row = (global::Config.Combat.DBookEntry)value; return new object[] { row.Name }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Combo", typeof(global::Config.Combat.DCombo),
            value => { var row = (global::Config.Combat.DCombo)value; return new object[] { row.Weapon, row.Tier, row.Airborne, row.Effect }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Command.Jump", typeof(global::Config.Combat.Command.DJump),
            value => { var row = (global::Config.Combat.Command.DJump)value; return new object[] { row.Key }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
                new RelationResolver.Plan("combat.encounter", 0, "key", true,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{0}),
                    }, "ancestor:combat.Command.StateMachine", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(0, 1, new int[]{0}),
                    }),
            }, (value, refs) => {
                var row = (global::Config.Combat.Command.DJump)value;
                row.NullableRefKey = (global::Config.Combat.DNestedPhase)refs[0]!;
            }),
        new RelationResolver.Shape("combat.Command.Noop", typeof(global::Config.Combat.Command.DNoop),
            value => { var row = (global::Config.Combat.Command.DNoop)value; return new object[] {  }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Command.StateMachine", typeof(global::Config.Combat.Command.DStateMachine),
            value => { var row = (global::Config.Combat.Command.DStateMachine)value; return new object[] { row.Phases }; },
            new RelationResolver.Index[] {
                new RelationResolver.Index(0, new int[]{0}),
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Effect.Damage", typeof(global::Config.Combat.Effect.DDamage),
            value => { var row = (global::Config.Combat.Effect.DDamage)value; return new object[] { row.Amount }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Effect.Heal", typeof(global::Config.Combat.Effect.DHeal),
            value => { var row = (global::Config.Combat.Effect.DHeal)value; return new object[] { row.Amount }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Effect.None", typeof(global::Config.Combat.Effect.DNone),
            value => { var row = (global::Config.Combat.Effect.DNone)value; return new object[] {  }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Exit", typeof(global::Config.Combat.DExit),
            value => { var row = (global::Config.Combat.DExit)value; return new object[] { row.Effect }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Frame", typeof(global::Config.Combat.DFrame),
            value => { var row = (global::Config.Combat.DFrame)value; return new object[] { row.Tick, row.Effect }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Impact", typeof(global::Config.Combat.DImpact),
            value => { var row = (global::Config.Combat.DImpact)value; return new object[] { row.Ordinal, row.Damage }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Machine", typeof(global::Config.Combat.DMachine),
            value => { var row = (global::Config.Combat.DMachine)value; return new object[] { row.Code, row.Phases, row.Impacts, row.Frames, row.Stances, row.Variants, row.Combos, row.Markers, row.Anchor, row.Anchors, row.OnExit, row.Initial, row.Impact, row.Frame, row.Stance, row.Variant, row.Weapon, row.Tier, row.Airborne }; },
            new RelationResolver.Index[] {
                new RelationResolver.Index(1, new int[]{0}),
                new RelationResolver.Index(2, new int[]{0}),
                new RelationResolver.Index(3, new int[]{0}),
                new RelationResolver.Index(4, new int[]{0}),
                new RelationResolver.Index(5, new int[]{0}),
                new RelationResolver.Index(6, new int[]{0, 1, 2}),
                new RelationResolver.Index(7, new int[]{0}),
            },
            new RelationResolver.Plan[] {
                new RelationResolver.Plan("combat.ability", 0, "initial", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{11}),
                    }, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(1, 1, new int[]{0}),
                    }),
                new RelationResolver.Plan("combat.ability", 1, "impact", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{12}),
                    }, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(2, 1, new int[]{0}),
                    }),
                new RelationResolver.Plan("combat.ability", 2, "frame", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{13}),
                    }, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(3, 1, new int[]{0}),
                    }),
                new RelationResolver.Plan("combat.ability", 3, "stance", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{14}),
                    }, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(4, 1, new int[]{0}),
                    }),
                new RelationResolver.Plan("combat.ability", 4, "variant", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{15}),
                    }, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(5, 1, new int[]{0}),
                    }),
                new RelationResolver.Plan("combat.ability", 5, "combo", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{16}),
                        new RelationResolver.Source("", new int[]{17}),
                        new RelationResolver.Source("", new int[]{18}),
                    }, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(6, 1, new int[]{0, 1, 2}),
                    }),
            }, (value, refs) => {
                var row = (global::Config.Combat.DMachine)value;
                row.RefInitial = (global::Config.Combat.DPhase)refs[0]!;
                row.RefImpact = (global::Config.Combat.DImpact)refs[1]!;
                row.RefFrame = (global::Config.Combat.DFrame)refs[2]!;
                row.RefStance = (global::Config.Combat.DStance)refs[3]!;
                row.RefVariant = (global::Config.Combat.DPhaseVariant)refs[4]!;
                row.RefCombo = (global::Config.Combat.DCombo)refs[5]!;
            }),
        new RelationResolver.Shape("combat.MachineUse", typeof(global::Config.Combat.DMachineUse),
            value => { var row = (global::Config.Combat.DMachineUse)value; return new object[] { row.MachineKey, row.Targets }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Marker", typeof(global::Config.Combat.DMarker),
            value => { var row = (global::Config.Combat.DMarker)value; return new object[] { row.Key }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.NestedPhase", typeof(global::Config.Combat.DNestedPhase),
            value => { var row = (global::Config.Combat.DNestedPhase)value; return new object[] { row.Key, row.Action }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.OptionalAim", typeof(global::Config.Combat.DOptionalAim),
            value => { var row = (global::Config.Combat.DOptionalAim)value; return new object[] { row.AbilityId, row.MachineKey, row.PhaseKey }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
                new RelationResolver.Plan("combat.cast", 0, "target", true,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{0}),
                        new RelationResolver.Source("", new int[]{1}),
                        new RelationResolver.Source("", new int[]{2}),
                    }, "table:combat.ability", new int[]{0}, new RelationResolver.Step[] {
                        new RelationResolver.Step(3, 1, new int[]{0}),
                        new RelationResolver.Step(1, 1, new int[]{0}),
                    }),
            }, (value, refs) => {
                var row = (global::Config.Combat.DOptionalAim)value;
                row.NullableRefTarget = (global::Config.Combat.DPhase)refs[0]!;
            }),
        new RelationResolver.Shape("combat.Origin", typeof(global::Config.Combat.DOrigin),
            value => { var row = (global::Config.Combat.DOrigin)value; return new object[] { row.X, row.Y }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Phase", typeof(global::Config.Combat.DPhase),
            value => { var row = (global::Config.Combat.DPhase)value; return new object[] { row.Key, row.Next }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
                new RelationResolver.Plan("combat.ability", 0, "next", true,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{1}),
                    }, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(1, 1, new int[]{0}),
                    }),
                new RelationResolver.Plan("combat.ability", 1, "exit", false,
                    new RelationResolver.Source[] {
                    }, "ancestor:combat.Machine", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(10, 0, new int[]{}),
                        new RelationResolver.Step(0, 0, new int[]{}),
                    }),
            }, (value, refs) => {
                var row = (global::Config.Combat.DPhase)value;
                row.NullableRefNext = (global::Config.Combat.DPhase)refs[0]!;
                row.RefExit = (global::Config.Combat.DEffect)refs[1]!;
            }),
        new RelationResolver.Shape("combat.PhaseKey", typeof(global::Config.Combat.DPhaseKey),
            value => { var row = (global::Config.Combat.DPhaseKey)value; return new object[] { row.Name, row.Variant }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.PhaseUse", typeof(global::Config.Combat.DPhaseUse),
            value => { var row = (global::Config.Combat.DPhaseUse)value; return new object[] { row.PhaseKey }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
                new RelationResolver.Plan("combat.cast", 0, "target", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("ancestor:combat.AbilityUse", new int[]{0, 0}),
                        new RelationResolver.Source("ancestor:combat.MachineUse", new int[]{0}),
                        new RelationResolver.Source("", new int[]{0}),
                    }, "table:combat.ability", new int[]{0}, new RelationResolver.Step[] {
                        new RelationResolver.Step(3, 1, new int[]{0}),
                        new RelationResolver.Step(1, 1, new int[]{0}),
                    }),
                new RelationResolver.Plan("combat.cast", 1, "byCode", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("root", new int[]{1}),
                        new RelationResolver.Source("ancestor:combat.MachineUse", new int[]{0}),
                        new RelationResolver.Source("", new int[]{0}),
                    }, "table:combat.ability", new int[]{1}, new RelationResolver.Step[] {
                        new RelationResolver.Step(3, 1, new int[]{0}),
                        new RelationResolver.Step(1, 1, new int[]{0}),
                    }),
                new RelationResolver.Plan("combat.cast", 2, "origin", false,
                    new RelationResolver.Source[] {
                    }, "root", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(2, 0, new int[]{}),
                    }),
            }, (value, refs) => {
                var row = (global::Config.Combat.DPhaseUse)value;
                row.RefTarget = (global::Config.Combat.DPhase)refs[0]!;
                row.RefByCode = (global::Config.Combat.DPhase)refs[1]!;
                row.RefOrigin = (global::Config.Combat.DOrigin)refs[2]!;
            }),
        new RelationResolver.Shape("combat.PhaseVariant", typeof(global::Config.Combat.DPhaseVariant),
            value => { var row = (global::Config.Combat.DPhaseVariant)value; return new object[] { row.Key, row.Effect }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Reward", typeof(global::Config.Combat.DReward),
            value => { var row = (global::Config.Combat.DReward)value; return new object[] { row.Count }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.Stance", typeof(global::Config.Combat.DStance),
            value => { var row = (global::Config.Combat.DStance)value; return new object[] { row.Airborne, row.Effect }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.ability", typeof(global::Config.Combat.DAbility),
            value => { var row = (global::Config.Combat.DAbility)value; return new object[] { row.Id, row.Code, row.Origin, row.Machines, row.StartMachine, row.StartPhase }; },
            new RelationResolver.Index[] {
                new RelationResolver.Index(3, new int[]{0}),
            },
            new RelationResolver.Plan[] {
                new RelationResolver.Plan("combat.ability", 0, "initial", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{4}),
                        new RelationResolver.Source("", new int[]{5}),
                    }, "root", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(3, 1, new int[]{0}),
                        new RelationResolver.Step(1, 1, new int[]{0}),
                    }),
                new RelationResolver.Plan("combat.ability", 1, "spawn", false,
                    new RelationResolver.Source[] {
                    }, "root", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(2, 0, new int[]{}),
                    }),
            }, (value, refs) => {
                var row = (global::Config.Combat.DAbility)value;
                row.RefInitial = (global::Config.Combat.DPhase)refs[0]!;
                row.RefSpawn = (global::Config.Combat.DOrigin)refs[1]!;
            }),
        new RelationResolver.Shape("combat.branchreward", typeof(global::Config.Combat.DBranchreward),
            value => { var row = (global::Config.Combat.DBranchreward)value; return new object[] { row.Key, row.Reward }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.cast", typeof(global::Config.Combat.DCast),
            value => { var row = (global::Config.Combat.DCast)value; return new object[] { row.Id, row.AbilityCode, row.Origin, row.Uses, row.Optional, row.School, row.Tier, row.Airborne, row.BookCode, row.Edition, row.RewardKey }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
                new RelationResolver.Plan("combat.cast", 0, "book", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{5}),
                        new RelationResolver.Source("", new int[]{6}),
                        new RelationResolver.Source("", new int[]{7}),
                    }, "table:combat.phasebook", new int[]{0, 1, 2}, new RelationResolver.Step[] {
                        new RelationResolver.Step(5, 0, new int[]{}),
                    }),
                new RelationResolver.Plan("combat.cast", 1, "bookByCode", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{8}),
                        new RelationResolver.Source("", new int[]{9}),
                    }, "table:combat.phasebook", new int[]{3, 4}, new RelationResolver.Step[] {
                        new RelationResolver.Step(5, 0, new int[]{}),
                    }),
                new RelationResolver.Plan("combat.cast", 2, "bookRecord", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("root", new int[]{5}),
                        new RelationResolver.Source("root", new int[]{6}),
                        new RelationResolver.Source("root", new int[]{7}),
                    }, "table:combat.phasebook", new int[]{0, 1, 2}, new RelationResolver.Step[] {
                    }),
                new RelationResolver.Plan("combat.cast", 3, "reward", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{10}),
                    }, "table:combat.branchreward", new int[]{0}, new RelationResolver.Step[] {
                        new RelationResolver.Step(1, 0, new int[]{}),
                    }),
            }, (value, refs) => {
                var row = (global::Config.Combat.DCast)value;
                row.RefBook = (global::Config.Combat.DBookEntry)refs[0]!;
                row.RefBookByCode = (global::Config.Combat.DBookEntry)refs[1]!;
                row.RefBookRecord = (global::Config.Combat.DPhasebook)refs[2]!;
                row.RefReward = (global::Config.Combat.DReward)refs[3]!;
            }),
        new RelationResolver.Shape("combat.encounter", typeof(global::Config.Combat.DEncounter),
            value => { var row = (global::Config.Combat.DEncounter)value; return new object[] { row.Id, row.Command }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.phasebook", typeof(global::Config.Combat.DPhasebook),
            value => { var row = (global::Config.Combat.DPhasebook)value; return new object[] { row.School, row.Tier, row.Airborne, row.Code, row.Edition, row.Entry }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("combat.timeline", typeof(global::Config.Combat.DTimeline),
            value => { var row = (global::Config.Combat.DTimeline)value; return new object[] { row.Id, RelationResolver.WrapMap(row.Events), RelationResolver.WrapMap(row.Channels), RelationResolver.WrapMap(row.Slots), RelationResolver.WrapMap(row.Clock), RelationResolver.WrapMap(row.Modes), row.EventKey, row.Channel, row.Slot, row.Frame, row.Airborne, row.Fallback }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
                new RelationResolver.Plan("combat.timeline", 0, "eventKey", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{6}),
                    }, "root", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(1, 2, new int[]{}),
                    }),
                new RelationResolver.Plan("combat.timeline", 1, "channel", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{7}),
                    }, "root", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(2, 2, new int[]{}),
                    }),
                new RelationResolver.Plan("combat.timeline", 2, "slot", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{8}),
                    }, "root", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(3, 2, new int[]{}),
                    }),
                new RelationResolver.Plan("combat.timeline", 3, "frame", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{9}),
                    }, "root", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(4, 2, new int[]{}),
                    }),
                new RelationResolver.Plan("combat.timeline", 4, "airborne", false,
                    new RelationResolver.Source[] {
                        new RelationResolver.Source("", new int[]{10}),
                    }, "root", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(5, 2, new int[]{}),
                    }),
                new RelationResolver.Plan("combat.timeline", 5, "default", false,
                    new RelationResolver.Source[] {
                    }, "root", new int[]{}, new RelationResolver.Step[] {
                        new RelationResolver.Step(11, 0, new int[]{}),
                    }),
            }, (value, refs) => {
                var row = (global::Config.Combat.DTimeline)value;
                row.RefEventKey = (global::Config.Combat.DEffect)refs[0]!;
                row.RefChannel = (global::Config.Combat.DEffect)refs[1]!;
                row.RefSlot = (global::Config.Combat.DEffect)refs[2]!;
                row.RefFrame = (global::Config.Combat.DEffect)refs[3]!;
                row.RefAirborne = (global::Config.Combat.DEffect)refs[4]!;
                row.RefDefault = (global::Config.Combat.DEffect)refs[5]!;
            }),
        new RelationResolver.Shape("equip.TestPackBean", typeof(global::Config.Equip.DTestPackBean),
            value => { var row = (global::Config.Equip.DTestPackBean)value; return new object[] { row.Name, row.IRange }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("equip.ability", typeof(global::Config.Equip.DAbilityInfo),
            value => { var row = (global::Config.Equip.DAbilityInfo)value; return new object[] { row.Id, row.Name }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("equip.equip_config", typeof(global::Config.Equip.DEquipConfig),
            value => { var row = (global::Config.Equip.DEquipConfig)value; return new object[] { row.Entry, row.Stone_count_for_set, row.Draw_protect_name, row.Broadcastid, row.Broadcast_least_quality, row.Week_reward_mailid }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("equip.jewelry", typeof(global::Config.Equip.DJewelry),
            value => { var row = (global::Config.Equip.DJewelry)value; return new object[] { row.ID, row.Name, row.IconFile, row.LvlRank, row.JType, row.SuitID, row.KeyAbility, row.KeyAbilityValue, row.SalePrice, row.Description }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("equip.jewelry_random", typeof(global::Config.Equip.DJewelryRandom),
            value => { var row = (global::Config.Equip.DJewelryRandom)value; return new object[] { row.LvlRank, row.AttackRange, row.OtherRange, row.TestPack }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("equip.jewelry_suit", typeof(global::Config.Equip.DJewelrySuit),
            value => { var row = (global::Config.Equip.DJewelrySuit)value; return new object[] { row.SuitID, row.Ename, row.Name, row.Ability1, row.Ability1Value, row.Ability2, row.Ability2Value, row.Ability3, row.Ability3Value, row.SuitList }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("equip.jewelry_type", typeof(global::Config.Equip.DJewelryTypeInfo),
            value => { var row = (global::Config.Equip.DJewelryTypeInfo)value; return new object[] { row.TypeName }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("equip.rank", typeof(global::Config.Equip.DRankInfo),
            value => { var row = (global::Config.Equip.DRankInfo)value; return new object[] { row.RankID, row.RankName, row.RankShowName }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("other.ArgCaptureMode", typeof(global::Config.Other.DArgCaptureModeInfo),
            value => { var row = (global::Config.Other.DArgCaptureModeInfo)value; return new object[] { row.Name, row.Id, row.Comment }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("other.DropItem", typeof(global::Config.Other.DDropItem),
            value => { var row = (global::Config.Other.DDropItem)value; return new object[] { row.Chance, row.Itemids, row.Countmin, row.Countmax }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("other.drop", typeof(global::Config.Other.DDrop),
            value => { var row = (global::Config.Other.DDrop)value; return new object[] { row.Dropid, row.Name, row.Items, RelationResolver.WrapMap(row.Testmap) }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("other.key_test", typeof(global::Config.Other.DKeyTest),
            value => { var row = (global::Config.Other.DKeyTest)value; return new object[] { row.Id1, row.Id2, row.Id3, row.Ids, row.EnumTest, row.EnumList }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("other.loot", typeof(global::Config.Other.DLoot),
            value => { var row = (global::Config.Other.DLoot)value; return new object[] { row.Lootid, row.Ename, row.Name, row.ChanceList }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("other.loot_item", typeof(global::Config.Other.DLootItem),
            value => { var row = (global::Config.Other.DLootItem)value; return new object[] { row.Lootid, row.Itemid, row.Chance, row.Countmin, row.Countmax }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("other.monster", typeof(global::Config.Other.DMonster),
            value => { var row = (global::Config.Other.DMonster)value; return new object[] { row.Id, row.PosList, row.LootId, row.LootItemId, RelationResolver.WrapMap(row.EnumMap1), RelationResolver.WrapMap(row.EnumMap2) }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("other.sign_in", typeof(global::Config.Other.DSignIn),
            value => { var row = (global::Config.Other.DSignIn)value; return new object[] { row.Id, RelationResolver.WrapMap(row.Item2countMap), RelationResolver.WrapMap(row.Vipitem2vipcountMap), row.Viplevel, row.IconFile }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.TestDefaultBean", typeof(global::Config.Task.DTestDefaultBean),
            value => { var row = (global::Config.Task.DTestDefaultBean)value; return new object[] { row.TestInt, row.TestBool, row.TestString, row.TestSubBean, row.TestList, row.TestList2, RelationResolver.WrapMap(row.TestMap) }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.complete_condition_type", typeof(global::Config.Task.DCompleteConditionTypeInfo),
            value => { var row = (global::Config.Task.DCompleteConditionTypeInfo)value; return new object[] { row.Id, row.Name }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.completecondition.Chat", typeof(global::Config.Task.Completecondition.DChat),
            value => { var row = (global::Config.Task.Completecondition.DChat)value; return new object[] { row.Msg }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.completecondition.CollectItem", typeof(global::Config.Task.Completecondition.DCollectItem),
            value => { var row = (global::Config.Task.Completecondition.DCollectItem)value; return new object[] { row.Itemid, row.Count }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.completecondition.ConditionAnd", typeof(global::Config.Task.Completecondition.DConditionAnd),
            value => { var row = (global::Config.Task.Completecondition.DConditionAnd)value; return new object[] { row.Cond1, row.Cond2 }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.completecondition.KillMonster", typeof(global::Config.Task.Completecondition.DKillMonster),
            value => { var row = (global::Config.Task.Completecondition.DKillMonster)value; return new object[] { row.Monsterid, row.Count }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.completecondition.TalkNpc", typeof(global::Config.Task.Completecondition.DTalkNpc),
            value => { var row = (global::Config.Task.Completecondition.DTalkNpc)value; return new object[] { row.Npcid }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.completecondition.TestNoColumn", typeof(global::Config.Task.Completecondition.DTestNoColumn),
            value => { var row = (global::Config.Task.Completecondition.DTestNoColumn)value; return new object[] {  }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.completecondition.aa", typeof(global::Config.Task.Completecondition.DAa),
            value => { var row = (global::Config.Task.Completecondition.DAa)value; return new object[] {  }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.task", typeof(global::Config.Task.DTask),
            value => { var row = (global::Config.Task.DTask)value; return new object[] { row.Taskid, row.Name, row.Nexttask, row.Completecondition, row.Exp, row.TestDefaultBean }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.task2", typeof(global::Config.Task.DTask2),
            value => { var row = (global::Config.Task.DTask2)value; return new object[] { row.Taskid, row.Name, row.Nexttask, row.Completecondition, row.Exp, row.TestBool, row.TestString, row.TestStruct, row.TestList, row.TestListStruct, row.TestListInterface }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
        new RelationResolver.Shape("task.task_extra_exp", typeof(global::Config.Task.DTaskExtraExp),
            value => { var row = (global::Config.Task.DTaskExtraExp)value; return new object[] { row.Taskid, row.Extraexp, row.Test1, row.Test2, row.Fielda, row.Fieldb, row.Fieldc, row.Fieldd }; },
            new RelationResolver.Index[] {
            },
            new RelationResolver.Plan[] {
            }, (value, refs) => {
            }),
    });
    resolver.Table("ai.ai", global::Config.Ai.DAi.All(config));
    resolver.Table("ai.ai_action", global::Config.Ai.DAiAction.All(config));
    resolver.Table("ai.ai_condition", global::Config.Ai.DAiCondition.All(config));
    resolver.Table("combat.ability", global::Config.Combat.DAbility.All(config));
    resolver.Table("combat.branchreward", global::Config.Combat.DBranchreward.All(config));
    resolver.Table("combat.cast", global::Config.Combat.DCast.All(config));
    resolver.Table("combat.encounter", global::Config.Combat.DEncounter.All(config));
    resolver.Table("combat.phasebook", global::Config.Combat.DPhasebook.All(config));
    resolver.Table("combat.timeline", global::Config.Combat.DTimeline.All(config));
    resolver.Table("equip.ability", global::Config.Equip.DAbilityInfo.All(config));
    resolver.Table("equip.equip_config", global::Config.Equip.DEquipConfig.All(config));
    resolver.Table("equip.jewelry", global::Config.Equip.DJewelry.All(config));
    resolver.Table("equip.jewelry_random", global::Config.Equip.DJewelryRandom.All(config));
    resolver.Table("equip.jewelry_suit", global::Config.Equip.DJewelrySuit.All(config));
    resolver.Table("equip.jewelry_type", global::Config.Equip.DJewelryTypeInfo.All(config));
    resolver.Table("equip.rank", global::Config.Equip.DRankInfo.All(config));
    resolver.Table("other.ArgCaptureMode", global::Config.Other.DArgCaptureModeInfo.All(config));
    resolver.Table("other.drop", global::Config.Other.DDrop.All(config));
    resolver.Table("other.key_test", global::Config.Other.DKeyTest.All(config));
    resolver.Table("other.loot", global::Config.Other.DLoot.All(config));
    resolver.Table("other.loot_item", global::Config.Other.DLootItem.All(config));
    resolver.Table("other.monster", global::Config.Other.DMonster.All(config));
    resolver.Table("other.sign_in", global::Config.Other.DSignIn.All(config));
    resolver.Table("task.complete_condition_type", global::Config.Task.DCompleteConditionTypeInfo.All(config));
    resolver.Table("task.task", global::Config.Task.DTask.All(config));
    resolver.Table("task.task2", global::Config.Task.DTask2.All(config));
    resolver.Table("task.task_extra_exp", global::Config.Task.DTaskExtraExp.All(config));
    resolver.Resolve();
}

    private sealed class RelationResolver
{
    internal sealed record Source(string Scope, int[] Fields);
    internal sealed record Step(int Field, int Kind, int[] Keys);
    internal sealed record Index(int Field, int[] Keys);
    internal sealed record Plan(string Root, int Slot, string Name, bool Nullable, Source[] Sources, string Target, int[] RecordKey, Step[] Steps);
    internal sealed record Shape(string Name, Type RuntimeType, Func<object, object[]> Fields, Index[] Indexes, Plan[] Plans, Action<object, object?[]> Bind);
    private sealed record Occurrence(object Value, object Root, string Table, Occurrence? Parent);
    internal static object WrapMap<K, V>(IReadOnlyDictionary<K, V> map) where K : notnull
    {
        var view = new Dictionary<object, object>();
        foreach (var pair in map) view.Add(pair.Key, pair.Value!);
        return view;
    }
    private sealed class Identity : IEqualityComparer<object>
    {
        public new bool Equals(object? a, object? b) => ReferenceEquals(a, b);
        public int GetHashCode(object value) => System.Runtime.CompilerServices.RuntimeHelpers.GetHashCode(value);
    }
    private sealed class Key : IEquatable<Key>
    {
        private readonly object[] _parts;
        internal Key(object[] parts) { _parts = parts; }
        public bool Equals(Key? other) => other != null && System.Linq.Enumerable.SequenceEqual(_parts, other._parts);
        public override bool Equals(object? other) => other is Key key && Equals(key);
        public override int GetHashCode() { int hash = 17; foreach (var part in _parts) hash = unchecked(hash * 31 + part.GetHashCode()); return hash; }
    }
    private readonly Dictionary<Type, Shape> _types = new Dictionary<Type, Shape>();
    private readonly Dictionary<string, System.Collections.IEnumerable> _tables = new Dictionary<string, System.Collections.IEnumerable>();
    private readonly Dictionary<object, object[]> _fields = new Dictionary<object, object[]>(new Identity());
    private readonly Dictionary<object, Dictionary<string, Dictionary<Key, object>>> _indexes = new Dictionary<object, Dictionary<string, Dictionary<Key, object>>>(new Identity());
    private readonly List<Occurrence> _occurrences = new List<Occurrence>();
    private readonly LoadErrors _errors;

    internal RelationResolver(LoadErrors errors, Shape[] shapes)
    {
        _errors = errors;
        foreach (var shape in shapes) _types.Add(shape.RuntimeType, shape);
    }
    internal void Table(string name, System.Collections.IEnumerable rows) { _tables.Add(name, rows); }
    private object[] Read(object value)
    {
        if (!_fields.TryGetValue(value, out var fields)) {
            fields = _types[value.GetType()].Fields(value);
            _fields.Add(value, fields);
        }
        return fields;
    }

    internal void Resolve()
    {
        foreach (var table in _tables) foreach (object row in table.Value) Walk(row, row, table.Key, null);
        _errors.CheckErrors();
        foreach (var at in _occurrences) {
            var shape = _types[at.Value.GetType()];
            int slots = 0; foreach (var plan in shape.Plans) slots = Math.Max(slots, plan.Slot + 1);
            var refs = new object?[slots];
            foreach (var plan in shape.Plans) {
                if (plan.Root != at.Table) continue;
                refs[plan.Slot] = Bind(at, shape, plan);
            }
            shape.Bind(at.Value, refs);
        }
    }

    private object? Bind(Occurrence at, Shape shape, Plan plan)
    {
        var keys = new List<object>(plan.Sources.Length);
        bool valid = true;
        foreach (var source in plan.Sources) {
            object? value = Scope(source.Scope, at);
            if (value == null) { valid = false; continue; }
            foreach (int field in source.Fields) value = Read(value)[field];
            keys.Add(value);
        }
        bool external = plan.Target.StartsWith("table:", StringComparison.Ordinal);
        object? target = external ? _tables[plan.Target.Substring(6)] : Scope(plan.Target, at);
        if (!valid || target == null) {
            _errors.MissingReference(shape.Name, plan.Name, "scope", at.Table);
            return null;
        }
        int used = 0;
        if (external) {
            Lookup((System.Collections.IEnumerable)target, plan.RecordKey, plan.Target)
                .TryGetValue(new Key(keys.GetRange(0, plan.RecordKey.Length).ToArray()), out target);
            used = plan.RecordKey.Length;
        }
        foreach (var step in plan.Steps) {
            if (target == null) break;
            object member = Read(target)[step.Field];
            if (step.Kind == 0) target = member;
            else if (step.Kind == 1) Lookup((System.Collections.IEnumerable)member, step.Keys, shape.Name)
                .TryGetValue(new Key(keys.GetRange(used, step.Keys.Length).ToArray()), out target);
            else ((Dictionary<object, object>)member).TryGetValue(keys[used], out target);
            used += step.Kind == 2 ? 1 : step.Keys.Length;
        }
        if (target == null && !plan.Nullable) _errors.MissingReference(shape.Name, plan.Name, plan.Target, keys);
        return target;
    }

    private object? Scope(string scope, Occurrence at)
    {
        if (scope.Length == 0) return at.Value;
        if (scope == "root") return at.Root;
        for (Occurrence? current = at; current != null; current = current.Parent) {
            object value = current.Value;
            if ("ancestor:" + _types[value.GetType()].Name == scope) return value;
        }
        return null;
    }

    private void Walk(object value, object root, string table, Occurrence? parent)
    {
        if (_types.TryGetValue(value.GetType(), out var shape)) {
            var at = new Occurrence(value, root, table, parent);
            if (shape.Plans.Length != 0) _occurrences.Add(at);
            var fields = Read(value);
            foreach (var index in shape.Indexes) Lookup((System.Collections.IEnumerable)fields[index.Field], index.Keys, shape.Name);
            foreach (var field in fields) Walk(field, root, table, at);
        }
        else if (value is Dictionary<object, object> map) { foreach (var pair in map) { Walk(pair.Key, root, table, parent); Walk(pair.Value, root, table, parent); } }
        else if (value is System.Collections.IEnumerable list && value is not string) { foreach (object item in list) Walk(item, root, table, parent); }
    }

    private Dictionary<Key, object> Lookup(System.Collections.IEnumerable rows, int[] keys, string name)
    {
        if (!_indexes.TryGetValue(rows, out var definitions)) { definitions = new Dictionary<string, Dictionary<Key, object>>(); _indexes.Add(rows, definitions); }
        string definition = string.Join(",", keys);
        if (definitions.TryGetValue(definition, out var index)) return index;
        index = new Dictionary<Key, object>(); definitions.Add(definition, index);
        foreach (object row in rows) {
            var fields = Read(row); var parts = new object[keys.Length];
            for (int i = 0; i < keys.Length; i++) parts[i] = fields[keys[i]];
            if (!index.TryAdd(new Key(parts), row)) _errors.DuplicateKey(name, parts);
        }
        return index;
    }
}

    private static void Ready(global::Config.Ai.DAi value, ConfigMgr config)
    {
        Ready(value.TrigTick, config);
        Ready<global::Config.Ai.DAi>(value, config);
    }

    private static void Ready(global::Config.Combat.DExit value, ConfigMgr config)
    {
        Ready(value.Effect, config);
        Ready<global::Config.Combat.DExit>(value, config);
    }

    private static void Ready(global::Config.Combat.DFrame value, ConfigMgr config)
    {
        Ready(value.Effect, config);
        Ready<global::Config.Combat.DFrame>(value, config);
    }

    private static void Ready(global::Config.Combat.DStance value, ConfigMgr config)
    {
        Ready(value.Effect, config);
        Ready<global::Config.Combat.DStance>(value, config);
    }

    private static void Ready(global::Config.Combat.DPhaseVariant value, ConfigMgr config)
    {
        Ready(value.Key, config);
        Ready(value.Effect, config);
        Ready<global::Config.Combat.DPhaseVariant>(value, config);
    }

    private static void Ready(global::Config.Combat.DCombo value, ConfigMgr config)
    {
        Ready(value.Effect, config);
        Ready<global::Config.Combat.DCombo>(value, config);
    }

    private static void Ready(global::Config.Combat.DMachine value, ConfigMgr config)
    {
        foreach (var item in value.Phases) {
            Ready(item, config);
        }
        foreach (var item in value.Impacts) {
            Ready(item, config);
        }
        foreach (var item in value.Frames) {
            Ready(item, config);
        }
        foreach (var item in value.Stances) {
            Ready(item, config);
        }
        foreach (var item in value.Variants) {
            Ready(item, config);
        }
        foreach (var item in value.Combos) {
            Ready(item, config);
        }
        foreach (var item in value.Markers) {
            Ready(item, config);
        }
        Ready(value.Anchor, config);
        foreach (var item in value.Anchors) {
            Ready(item, config);
        }
        Ready(value.OnExit, config);
        Ready(value.Variant, config);
        Ready<global::Config.Combat.DMachine>(value, config);
    }

    private static void Ready(global::Config.Combat.DAbility value, ConfigMgr config)
    {
        Ready(value.Origin, config);
        foreach (var item in value.Machines) {
            Ready(item, config);
        }
        Ready<global::Config.Combat.DAbility>(value, config);
    }

    private static void Ready(global::Config.Combat.DMachineUse value, ConfigMgr config)
    {
        foreach (var item in value.Targets) {
            Ready(item, config);
        }
        Ready<global::Config.Combat.DMachineUse>(value, config);
    }

    private static void Ready(global::Config.Combat.DAbilityUse value, ConfigMgr config)
    {
        Ready(value.Binding, config);
        foreach (var item in value.Machines) {
            Ready(item, config);
        }
        Ready<global::Config.Combat.DAbilityUse>(value, config);
    }

    private static void Ready(global::Config.Combat.DPhasebook value, ConfigMgr config)
    {
        Ready(value.Entry, config);
        Ready<global::Config.Combat.DPhasebook>(value, config);
    }

    private static void Ready(global::Config.Combat.DBranchreward value, ConfigMgr config)
    {
        Ready(value.Key, config);
        Ready(value.Reward, config);
        Ready<global::Config.Combat.DBranchreward>(value, config);
    }

    private static void Ready(global::Config.Combat.DCast value, ConfigMgr config)
    {
        Ready(value.Origin, config);
        foreach (var item in value.Uses) {
            Ready(item, config);
        }
        foreach (var item in value.Optional) {
            Ready(item, config);
        }
        Ready(value.RewardKey, config);
        Ready<global::Config.Combat.DCast>(value, config);
    }

    private static void Ready(global::Config.Combat.DTimeline value, ConfigMgr config)
    {
        foreach (var pair in value.Events) {
            Ready(pair.Key, config);
            Ready(pair.Value, config);
        }
        foreach (var pair in value.Channels) {
            Ready(pair.Value, config);
        }
        foreach (var pair in value.Slots) {
            Ready(pair.Value, config);
        }
        foreach (var pair in value.Clock) {
            Ready(pair.Value, config);
        }
        foreach (var pair in value.Modes) {
            Ready(pair.Value, config);
        }
        Ready(value.EventKey, config);
        Ready(value.Fallback, config);
        Ready<global::Config.Combat.DTimeline>(value, config);
    }

    private static void Ready(global::Config.Combat.Command.DStateMachine value, ConfigMgr config)
    {
        foreach (var item in value.Phases) {
            Ready(item, config);
        }
        Ready<global::Config.Combat.Command.DStateMachine>(value, config);
    }

    private static void Ready(global::Config.Combat.DNestedPhase value, ConfigMgr config)
    {
        Ready(value.Action, config);
        Ready<global::Config.Combat.DNestedPhase>(value, config);
    }

    private static void Ready(global::Config.Combat.DEncounter value, ConfigMgr config)
    {
        Ready(value.Command, config);
        Ready<global::Config.Combat.DEncounter>(value, config);
    }

    private static void Ready(global::Config.Equip.DTestPackBean value, ConfigMgr config)
    {
        Ready(value.IRange, config);
        Ready<global::Config.Equip.DTestPackBean>(value, config);
    }

    private static void Ready(global::Config.Equip.DJewelry value, ConfigMgr config)
    {
        Ready(value.LvlRank, config);
        Ready<global::Config.Equip.DJewelry>(value, config);
    }

    private static void Ready(global::Config.Equip.DJewelryRandom value, ConfigMgr config)
    {
        Ready(value.LvlRank, config);
        Ready(value.AttackRange, config);
        foreach (var item in value.OtherRange) {
            Ready(item, config);
        }
        foreach (var item in value.TestPack) {
            Ready(item, config);
        }
        Ready<global::Config.Equip.DJewelryRandom>(value, config);
    }

    private static void Ready(global::Config.Other.DDrop value, ConfigMgr config)
    {
        foreach (var item in value.Items) {
            Ready(item, config);
        }
        Ready<global::Config.Other.DDrop>(value, config);
    }

    private static void Ready(global::Config.Other.DMonster value, ConfigMgr config)
    {
        foreach (var item in value.PosList) {
            Ready(item, config);
        }
        Ready<global::Config.Other.DMonster>(value, config);
    }

    private static void Ready(global::Config.Task.DTestDefaultBean value, ConfigMgr config)
    {
        Ready(value.TestSubBean, config);
        Ready<global::Config.Task.DTestDefaultBean>(value, config);
    }

    private static void Ready(global::Config.Task.Completecondition.DConditionAnd value, ConfigMgr config)
    {
        Ready(value.Cond1, config);
        Ready(value.Cond2, config);
        Ready<global::Config.Task.Completecondition.DConditionAnd>(value, config);
    }

    private static void Ready(global::Config.Task.DTask value, ConfigMgr config)
    {
        Ready(value.Completecondition, config);
        Ready(value.TestDefaultBean, config);
        Ready<global::Config.Task.DTask>(value, config);
    }

    private static void Ready(global::Config.Task.DTask2 value, ConfigMgr config)
    {
        Ready(value.Completecondition, config);
        Ready(value.TestStruct, config);
        foreach (var item in value.TestListStruct) {
            Ready(item, config);
        }
        foreach (var item in value.TestListInterface) {
            Ready(item, config);
        }
        Ready<global::Config.Task.DTask2>(value, config);
    }

    private static void Ready(global::Config.Combat.DCommand value, ConfigMgr config)
    {
        if (value is global::Config.Combat.Command.DStateMachine item) {
            Ready(item, config);
        }
        else {
            Ready<global::Config.Combat.DCommand>(value, config);
        }
    }

    private static void Ready(global::Config.Task.DCompletecondition value, ConfigMgr config)
    {
        if (value is global::Config.Task.Completecondition.DConditionAnd item) {
            Ready(item, config);
        }
        else {
            Ready<global::Config.Task.DCompletecondition>(value, config);
        }
    }

    private static List<T> ReadTable<K, T>(ConfigPackage[] packages, string table,
        Func<ConfigReader, T> readRow, Func<T, K> primaryKey, Dictionary<K, List<string>> origins,
        LoadErrors errors) where K : notnull
    {
        var rows = new List<T>();
        var slots = new Dictionary<K, int>();
        string block = "." + table;
        foreach (var pack in packages) {
            if (!pack.HasBlock(block)) {
                continue;
            }
            bool isBase = ReferenceEquals(pack, packages[0]);
            string packName = pack.Name;
            var input = pack.Block(block); input.SetErrors(errors);
            int count = input.ReadCount();
            for (int j = 0; j < count; j++) {
                var row = readRow(input);
                K key = primaryKey(row);
                bool exists = slots.TryGetValue(key, out var slot);
                bool duplicate = exists;
                if (!isBase) {
                    if (!origins.TryGetValue(key, out var chain)) {
                        chain = new List<string>();
                        origins.Add(key, chain);
                    }
                    duplicate = chain.Count != 0 && chain[chain.Count - 1] == packName;
                    if (!duplicate) {
                        chain.Add(packName);
                    }
                }
                if (duplicate) {
                    errors.DuplicateRecord(table, packName, key);
                    continue;
                }
                if (exists) {
                    rows[slot] = row;
                } else {
                    slots.Add(key, rows.Count);
                    rows.Add(row);
                }
            }
            input.RequireEnd();
        }
        return rows;
    }
}
