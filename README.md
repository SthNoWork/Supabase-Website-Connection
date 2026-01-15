-- Add read access for anon role
CREATE POLICY "anon_select" ON xxxSchemaNamexxx.xxxTable_Namexxx
FOR SELECT
TO anon
USING (true);

-- Add insert access for anon role
CREATE POLICY "anon_insert" ON xxxSchemaNamexxx.xxxTable_Namexxx
FOR INSERT
TO anon
WITH CHECK (true);

-- Add update access for anon role  
CREATE POLICY "anon_update" ON xxxSchemaNamexxx.xxxTable_Namexxx
FOR UPDATE
TO anon
USING (true)
WITH CHECK (true);

modify the config.js to match ur server