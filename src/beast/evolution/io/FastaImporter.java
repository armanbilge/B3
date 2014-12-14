/*
 * FastaImporter.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2014 BEAST Developers
 *
 * BEAST is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BEAST.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.evolution.io;

import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.SimpleAlignment;
import beast.evolution.datatype.DataType;
import beast.evolution.sequence.Sequence;
import beast.evolution.sequence.SequenceList;
import beast.evolution.util.Taxon;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Class for importing PHYLIP sequential file format
 *
 * @version $Id: PhylipSequentialImporter.java,v 1.5 2005/05/24 20:25:56 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public class FastaImporter extends Importer implements SequenceImporter {

    public static final char FASTA_FIRST_CHAR = '>';
    
    /**
     * Constructor
     */
    public FastaImporter(Reader reader, DataType dataType) {
        this(reader, null, dataType);
    }

    public FastaImporter(Reader reader, Writer commentWriter, DataType dataType) {
        super(reader, commentWriter);
        setCommentDelimiters('\0', '\0', '\0');

        this.dataType = dataType;
    }

    /**
     * importAlignment.
     */
    public Alignment importAlignment() throws IOException, ImportException
    {
        SimpleAlignment alignment = null;

        try {
            // find fasta line start
            while (read() != FASTA_FIRST_CHAR) {
            }

            do {
                final String name = readLine().trim();
                StringBuffer seq = new StringBuffer();

                readSequence(seq, dataType, "" + FASTA_FIRST_CHAR, Integer.MAX_VALUE, "-", "?", "", "");

                if (alignment == null) {
                    alignment = new SimpleAlignment();
                }

                alignment.addSequence(new Sequence(new Taxon(name.toString()), seq.toString()));

            } while (getLastDelimiter() == FASTA_FIRST_CHAR);
        } catch (EOFException e) {
            // catch end of file the ugly way.
        }

        return alignment;
    }

    /**
     * importSequences.
     */
    public SequenceList importSequences() throws IOException, ImportException {
        return importAlignment();
    }

    private DataType dataType;
    private int maxNameLength = 10;
}