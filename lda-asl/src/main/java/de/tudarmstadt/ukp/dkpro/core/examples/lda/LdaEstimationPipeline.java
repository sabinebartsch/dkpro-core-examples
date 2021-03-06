/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.core.examples.lda;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.mallet.lda.MalletLdaTopicModelTrainer;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stopwordremover.StopWordRemover;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

/**
 * This is a simple example pipeline to estimate an LDA topic model. It uses four components:
 * <ol>
 * <li>A reader to read the input texts from disk: {@link TextReader}</li>
 * <li>A segmenter to split text into sentences and tokens: {@link OpenNlpSegmenter}</li>
 * <li>A stop word remover: {@link StopWordRemover}</li>
 * <li>The Mallet LDA topic model estimator: {@link MalletLdaTopicModelTrainer}</li>
 * </ol>
 * <p>
 * The resulting model is stored in the file {@code target/model.mallet}, as defined by the field {@code TARGET_FILE}.
 * <p>
 * The example makes use of two short documents which do not provide a sufficient amount of words to
 * produce meaningful results. However, by setting a different source, the pipeline can be used as is;
 * although the number of iterations (defined by the {@code ITERATIONS} field) should probably be
 * increased.
 * <p>
 * The result is a {@link cc.mallet.topics.ParallelTopicModel} and can be used by a
 * {@link MalletLdaTopicModelTrainer} as well as
 * in Mallet directly.
 * </p>
 * @see LdaInferencePipeline
 */
public class LdaEstimationPipeline
{
    protected static final File TARGET_FILE = new File("target/model.mallet");
    private static final String LANGUAGE = "en";
    private static final URL STOPWORD_FILE = LdaEstimationPipeline.class.getClassLoader()
            .getResource("stopwords_en.txt");
    private static final String DEFAULT_SOURCE_DIR = "src/main/resources/texts/*";
    private static final int ITERATIONS = 100;
    private static final int N_TOPICS = 10;

    public static void main(String[] args)
            throws IOException, UIMAException
    {
        String inputDir = args.length > 0 ? args[0] : DEFAULT_SOURCE_DIR;

        /* this is the annotation type that determines the "document" unit size */
        String coveringType = Sentence.class.getCanonicalName();

        CollectionReaderDescription reader = createReaderDescription(TextReader.class,
                TextReader.PARAM_SOURCE_LOCATION, inputDir,
                TextReader.PARAM_LANGUAGE, LANGUAGE);
        AnalysisEngineDescription segmenter = createEngineDescription(OpenNlpSegmenter.class);
        AnalysisEngineDescription stopwordRemover = createEngineDescription(StopWordRemover.class,
                StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE);
        AnalysisEngineDescription lda = createEngineDescription(MalletLdaTopicModelTrainer.class,
                MalletLdaTopicModelTrainer.PARAM_TARGET_LOCATION, TARGET_FILE,
                MalletLdaTopicModelTrainer.PARAM_N_ITERATIONS, ITERATIONS,
                MalletLdaTopicModelTrainer.PARAM_N_TOPICS, N_TOPICS,
                MalletLdaTopicModelTrainer.PARAM_COVERING_ANNOTATION_TYPE, coveringType);

        SimplePipeline.runPipeline(reader, segmenter, stopwordRemover, lda);
    }
}
